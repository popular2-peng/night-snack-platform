package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.MerchantRegisterDTO;
import com.nsp.pojo.dto.MerchantShopUpdateDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantApplication;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.MerchantApplicationMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.cache.HomeMerchantListCache;
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
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
    private final HomeMerchantListCache homeMerchantListCache;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PageVO<MerchantVO> listNearby(BigDecimal lat, BigDecimal lng, int page, int size) {
        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus, "OPEN"));

        List<MerchantVO> sorted = merchants.stream()
                .map(m -> {
                    MerchantVO vo = toVO(m);
                    long dist = mapService.calculateDistanceWithCache(
                            m.getLat(), m.getLng(), lat, lng, m.getId());
                    vo.setDistance((int) dist);
                    return vo;
                })
                .sorted(Comparator.comparingInt(MerchantVO::getDistance))
                .toList();

        // 全量距离排序后内存分页（商家规模小，可接受；规模增大后可改为排序结果缓存 + 分页）
        int total = sorted.size();
        int offset = (page - 1) * size;
        if (offset >= total) {
            return new PageVO<>((long) total, List.of());
        }
        List<MerchantVO> pageList = sorted.subList(offset, Math.min(offset + size, total));
        return new PageVO<>((long) total, pageList);
    }

    @Override
    public PageVO<MerchantVO> listAll(int page, int size, Long categoryId, String keyword) {
        // Cache Aside：先读缓存，未命中回源 MySQL 并写入缓存
        List<MerchantVO> all = homeMerchantListCache.get();
        if (all == null) {
            all = loadOpenMerchants();
            homeMerchantListCache.put(all);
        }

        // 分类筛选（按商家主营品类匹配，内存过滤，避免按品类拆分缓存）
        if (categoryId != null) {
            all = all.stream()
                    .filter(m -> categoryId.equals(m.getCategoryId()))
                    .toList();
        }

        // 店铺名关键词搜索（搜索页使用，服务端过滤而非前端过滤）
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            all = all.stream()
                    .filter(m -> m.getName() != null && m.getName().toLowerCase().contains(kw))
                    .toList();
        }

        // 内存分页（商家规模小，全量缓存 + 内存分页可接受）
        int total = all.size();
        int offset = (page - 1) * size;
        if (offset >= total) {
            return new PageVO<>((long) total, List.of());
        }
        List<MerchantVO> pageList = all.subList(offset, Math.min(offset + size, total));
        return new PageVO<>((long) total, pageList);
    }

    /** 查询全部营业中商家（缓存回源用，按入驻时间倒序） */
    private List<MerchantVO> loadOpenMerchants() {
        return merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus, "OPEN")
                        .orderByDesc(Merchant::getId))
                .stream().map(this::toVO).toList();
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
        // 营业状态变更会影响首页商家列表，清除缓存
        homeMerchantListCache.evict();
        log.info("商家状态更新: merchantId={}, status={}", merchantId, status);
    }

    @Override
    public MerchantVO updateShop(Long merchantId, MerchantShopUpdateDTO dto) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.MERCHANT_NOT_FOUND_MSG);
        }

        // 营业时间必须成对填写（只传一个视为参数错误）
        boolean hasStart = dto.getBusinessHoursStart() != null && !dto.getBusinessHoursStart().isBlank();
        boolean hasEnd = dto.getBusinessHoursEnd() != null && !dto.getBusinessHoursEnd().isBlank();
        if (hasStart != hasEnd) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.MERCHANT_HOURS_PAIR_MSG);
        }

        // 部分更新：仅更新非空字段
        if (dto.getName() != null && !dto.getName().isBlank()) {
            merchant.setName(dto.getName().trim());
        }
        if (dto.getLogoUrl() != null && !dto.getLogoUrl().isBlank()) {
            merchant.setLogoUrl(dto.getLogoUrl());
        }
        if (dto.getDescription() != null) {
            merchant.setDescription(dto.getDescription());
        }
        if (dto.getNotice() != null) {
            merchant.setNotice(dto.getNotice());
        }
        if (dto.getDeliveryRadius() != null) {
            merchant.setDeliveryRadius(dto.getDeliveryRadius());
        }
        if (hasStart) {
            merchant.setBusinessHoursStart(LocalTime.parse(dto.getBusinessHoursStart()));
            merchant.setBusinessHoursEnd(LocalTime.parse(dto.getBusinessHoursEnd()));
        }

        // 地址变更 → 百度地理编码重新解析坐标；解析失败保留原坐标并告警（不阻断更新）
        if (dto.getAddress() != null && !dto.getAddress().isBlank()
                && !dto.getAddress().trim().equals(merchant.getAddress())) {
            String newAddress = dto.getAddress().trim();
            merchant.setAddress(newAddress);
            BigDecimal[] location = mapService.geocode(null, null, null, newAddress);
            if (location != null) {
                merchant.setLat(location[0]);
                merchant.setLng(location[1]);
                log.info("店铺地址更新并重新解析坐标: merchantId={}, address={}, lat={}, lng={}",
                        merchantId, newAddress, location[0], location[1]);
            } else {
                log.warn("店铺地址更新但地理编码失败，保留原坐标: merchantId={}, address={}", merchantId, newAddress);
            }
        }

        merchantMapper.updateById(merchant);
        log.info("店铺信息更新: merchantId={}, name={}", merchantId, merchant.getName());
        return toVO(merchant);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 商家登录仅用手机号 + 密码（系统无 username）
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, dto.getPhone()));

        if (merchant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_PHONE_NOT_MERCHANT_MSG);
        }
        if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
        }
        if ("SUSPENDED".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_ALREADY_BANNED, MessageConst.MERCHANT_BANNED_MSG);
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
                .username(merchant.getName())
                .role("MERCHANT")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public void register(MerchantRegisterDTO dto) {
        // 1. 校验手机号唯一（user 表 + merchant 表，登录凭证全局唯一）
        Long userPhoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        Long merchantPhoneCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, dto.getPhone()));
        if (userPhoneCount + merchantPhoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, MessageConst.PHONE_EXISTS_LOGIN_MSG);
        }

        // 2. 校验主营品类存在且启用
        PlatformCategory category = platformCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getDeleted() == 1) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, MessageConst.CATEGORY_NOT_EXISTS_MSG);
        }
        if (!"ENABLED".equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, MessageConst.CATEGORY_DISABLED_MSG);
        }

        // 3. 创建申请人 user 账号（username 由后端生成，规则与用户注册一致）
        User applicant = new User();
        applicant.setUsername("普通用户" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        applicant.setPassword(passwordEncoder.encode(dto.getPassword()));
        applicant.setPhone(dto.getPhone());
        applicant.setStatus("ACTIVE");
        userMapper.insert(applicant);

        // 4. 创建入驻申请（PENDING，密码已加密，审核通过后直接复用）
        MerchantApplication app = new MerchantApplication();
        app.setApplicantUserId(applicant.getId());
        app.setShopName(dto.getShopName());
        app.setPassword(applicant.getPassword());
        app.setCategoryId(dto.getCategoryId());
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

        log.info("商家入驻申请提交: applicantUserId={}, shopName={}, categoryId={}",
                applicant.getId(), dto.getShopName(), dto.getCategoryId());
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
