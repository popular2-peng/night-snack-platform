package com.nsp.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    /** 用户当前纬度（服务端以此为参考，实际以地址坐标或服务端计算为准） */
    private BigDecimal userLat;

    /** 用户当前经度 */
    private BigDecimal userLng;

    /**
     * 订单明细项
     */
    @Data
    public static class OrderItemDTO {

        @NotNull(message = "规格 ID 不能为空")
        private Long specId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

    }

}
