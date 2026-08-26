package com.nsp.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单请求参数
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "商家 ID 不能为空")
    private Long merchantId;

    @NotNull(message = "地址 ID 不能为空")
    private Long addressId;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /**
     * 幂等键（必填）：客户端生成的唯一标识（如 UUID），同一 clientToken 重复下单返回已创建的订单。
     * 服务端用 Redis setIfAbsent 拦截重复提交（key 有效期 10 分钟），并发重复请求返回 4006。
     */
    @NotBlank(message = "clientToken 不能为空")
    @Size(max = 64, message = "clientToken 最长 64 位")
    private String clientToken;

    /** 用户当前纬度（服务端以此为参考，实际以地址坐标或服务端计算为准） */
    private BigDecimal userLat;

    /** 用户当前经度 */
    private BigDecimal userLng;

    /**
     * 订单明细项 — productId 必填、specId 选填（有规格商品必须传，无规格商品不传）
     */
    @Data
    public static class OrderItemDTO {

        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        /** 规格 ID（选填）：有规格商品必传（按规格加价/扣库存），无规格商品不传（按商品基础价） */
        private Long specId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

    }

}
