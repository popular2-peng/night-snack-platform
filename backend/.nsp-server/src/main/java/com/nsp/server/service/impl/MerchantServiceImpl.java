package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
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
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "商家不存在");
        }
        return toVO(merchant);
    }

    @Override
    public void updateStatus(Long merchantId, Long userId, String status) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "商家不存在");
        }
        // 商家登录后 JWT 中的 userId 即为 merchant.id，校验一致性
        if (!merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, "非本店商家，无权操作");
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
            throw new BizException(ErrorCode.USER_NOT_FOUND, "该手机号未注册商家");
        }
        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, "密码错误");
        }
        if ("SUSPENDED".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "商家已被封禁");
        }
        if ("PENDING".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_NOT_APPROVED, "商家尚未通过审核");
        }

        log.info("商家登录: merchantId={}, phone={}", merchant.getId(), dto.getPhone());
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
            throw new BizException(ErrorCode.USERNAME_EXISTS, "账号已被占用，请更换");
        }

        // 2. 校验手机号唯一（user 表）
        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, "手机号已注册，请直接登录");
        }

        // 3. 校验主营品类存在且启用
        PlatformCategory category = platformCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getDeleted() == 1) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, "主营品类不存在");
        }
        if (!"ENABLED".equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, "主营品类已停用");
        }

        // 4. 创建申请人 user 账号
        User applicant = new User();
        applicant.setUsername(dto.getUsername());
        applicant.setPassword(passwordEncoder.encode(dto.getPassword()));
        applicant.setPhone(dto.getPhone());
        applicant.setNickname(dto.getShopName());
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
        app.setStatus("PENDING");
        merchantApplicationMapper.insert(app);

        log.info("商家入驻申请提交: applicantUserId={}, shopName={}, username={}, categoryId={}",
                applicant.getId(), dto.getShopName(), dto.getUsername(), dto.getCategoryId());
    }

    private MerchantVO toVO(Merchant merchant) {
        MerchantVO vo = new MerchantVO();
        BeanUtil.copyProperties(merchant, vo);
        vo.setStatus(merchant.getStatus());
        return vo;
    }

}
