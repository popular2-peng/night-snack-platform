package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起支付请求参数
 */
@Data
public class PayRequestDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "支付渠道不能为空（ALIPAY / WECHAT_MOCK）")
    private String channel;

}
