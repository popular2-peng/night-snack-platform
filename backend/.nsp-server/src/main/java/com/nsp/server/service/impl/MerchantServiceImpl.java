package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.MerchantRegisterDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantApplication;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.server.mapper.MerchantApplicationMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.security.JwtTokenProvider;
import com.nsp.server.service.MapService;
import com.nsp.server.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 商家业务实现 — 百度地图优先 + Haversine 降级 + Redis 距离缓存 + 商家入驻申请
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);

    private final MerchantMapper merchantMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;
    private final UserMapper userMapper;
    private final PlatformCategoryMapper platformCategoryMapper;
    private final MapService mapService;
    private final JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<MerchantVO> listNearby(BigDecimal lat, BigDecimal lng) {
        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus, "OPEN"));

        return merchants.stream()
                .map(m -> {
                    MerchantVO vo = toVO(m);
                    long dist = mapService.calculateDistanceWithCache(
                            m.getLat(), m.getLng(), lat, lng, m.getId());
                    vo.setDistance((int) dist);
                    return vo;
                })
                .sorted(Comparator.comparingInt(MerchantVO::getDistance))
                .toList();
    }

    @Override
    public MerchantVO getById(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.MERCHANT_NOT_FOUND_MSG);
        }
        return toVO(merchant);
    }

    @Override
    public void updateStatus(Long merchantId, Long userId, String status) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.MERCHANT_NOT_FOUND_MSG);
        }
        // 商家登录后 JWT 中的 userId 即为 merchant.id，校验一致性
        if (!merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.NOT_MERCHANT_OWNER_MSG);
        }

        Merchant update = new Merchant();
        update.setId(merchantId);
        update.setStatus(status.toUpperCase());
        merchantMapper.updateById(update);
        log.info("商家状态更新: merchantId={}, status={}", merchantId, status);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 手机号优先，兼容用户名
        Merchant merchant = null;
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, dto.getPhone()));
        } else if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUsername, dto.getUsername()));
        }

        if (merchant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_PHONE_NOT_MERCHANT_MSG);
        }
        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
        }
        if ("SUSPENDED".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_MERCHANT_BANNED_MSG);
        }
        if ("PENDING".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_NOT_APPROVED, MessageConst.MERCHANT_NOT_APPROVED_MSG);
        }

        log.info("商家登录: merchantId={}, phone={}", merchant.getId(), dto.getPhone());
        updateLastLogin(merchant.getId());
        String accessToken = jwtTokenProvider.createAccessToken(merchant.getId(), "MERCHANT");
        String refreshToken = jwtTokenProvider.createRefreshToken(merchant.getId(), "MERCHANT");
        return LoginVO.builder()
                .userId(merchant.getId())
                .username(merchant.getUsername())
                .role("MERCHANT")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public void register(MerchantRegisterDTO dto) {
        // 1. 校验用户名唯一（user 表 + 申请表 + 商家表）
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        Long appCount = merchantApplicationMapper.selectCount(
                new LambdaQueryWrapper<MerchantApplication>().eq(MerchantApplication::getUsername, dto.getUsername()));
        Long merCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUsername, dto.getUsername()));
        if (userCount + appCount + merCount > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, MessageConst.USERNAME_TAKEN_MSG);
        }

        // 2. 校验手机号唯一（user 表）
        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, MessageConst.PHONE_EXISTS_LOGIN_MSG);
        }

        // 3. 校验主营品类存在且启用
        PlatformCategory category = platformCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getDeleted() == 1) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, MessageConst.CATEGORY_NOT_EXISTS_MSG);
        }
        if (!"ENABLED".equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, MessageConst.CATEGORY_DISABLED_MSG);
        }

        // 4. 创建申请人 user 账号
        User applicant = new User();
        applicant.setUsername(dto.getUsername());
        applicant.setPassword(passwordEncoder.encode(dto.getPassword()));
        applicant.setPhone(dto.getPhone());
        applicant.setStatus("ACTIVE");
        userMapper.insert(applicant);

        // 5. 创建入驻申请（PENDING，密码已加密，审核通过后直接复用）
        MerchantApplication app = new MerchantApplication();
        app.setApplicantUserId(applicant.getId());
        app.setShopName(dto.getShopName());
        app.setUsername(dto.getUsername());
        app.setPassword(applicant.getPassword());
        app.setCategoryId(dto.getCategoryId());
        app.setReason(dto.getReason());
        app.setDescription(dto.getDescription());
        app.setLogoUrl(dto.getLogoUrl());
        // 联系人 + 地址信息（审核通过后直接用于创建商家）
        app.setOwnerName(dto.getOwnerName());
        app.setProvince(dto.getProvince());
        app.setCity(dto.getCity());
        app.setDistrict(dto.getDistrict());
        app.setAddress(dto.getAddress());
        // 调用百度地图地理编码获取坐标（AK 未配置/失败时 lat/lng 为 null，申请仍可提交）
        BigDecimal[] location = mapService.geocode(
                dto.getProvince(), dto.getCity(), dto.getDistrict(), dto.getAddress());
        if (location != null) {
            app.setLat(location[0]);
            app.setLng(location[1]);
        } else {
            log.warn("入驻申请地理编码失败，坐标待补充: shopName={}", dto.getShopName());
        }
        app.setStatus("PENDING");
        merchantApplicationMapper.insert(app);

        log.info("商家入驻申请提交: applicantUserId={}, shopName={}, username={}, categoryId={}",
                applicant.getId(), dto.getShopName(), dto.getUsername(), dto.getCategoryId());
    }

    /**
     * 商家登录成功后更新最后登录时间（last_login_at）
     *
     * <p>显式 `updated_at = updated_at` 阻止 MySQL ON UPDATE CURRENT_TIMESTAMP 自动刷新，
     * 避免「最后登录时间」与「修改时间」变成同一时刻（修改时间仅应在修改店铺信息时变化）。
     */
    private void updateLastLogin(Long merchantId) {
        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .set(Merchant::getLastLoginAt, LocalDateTime.now())
                .setSql("updated_at = updated_at"));
    }

    private MerchantVO toVO(Merchant merchant) {
        MerchantVO vo = new MerchantVO();
        BeanUtil.copyProperties(merchant, vo);
        vo.setStatus(merchant.getStatus());
        return vo;
    }

}
