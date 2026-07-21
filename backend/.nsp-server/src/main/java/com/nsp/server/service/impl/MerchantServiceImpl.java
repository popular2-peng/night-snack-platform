package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.enums.MerchantStatus;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.service.MapService;
import com.nsp.server.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 商家业务实现 — 百度地图优先 + Haversine 降级 + Redis 距离缓存
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);

    private final MerchantMapper merchantMapper;
    private final MapService mapService;

    @Override
    public List<MerchantVO> listNearby(BigDecimal lat, BigDecimal lng) {
        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus, MerchantStatus.OPEN));

        // 计算距离并按距离排序
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
        if (!merchant.getOwnerUserId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, "非本店商家，无权操作");
        }

        Merchant update = new Merchant();
        update.setId(merchantId);
        update.setStatus(MerchantStatus.fromCode(status));
        merchantMapper.updateById(update);
        log.info("商家状态更新: merchantId={}, status={}", merchantId, status);
    }

    private MerchantVO toVO(Merchant merchant) {
        MerchantVO vo = new MerchantVO();
        BeanUtil.copyProperties(merchant, vo);
        vo.setStatus(merchant.getStatus().getCode());
        return vo;
    }

}
