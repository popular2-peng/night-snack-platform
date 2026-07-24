package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 商家统计概览 — 各状态计数
 */
@Data
@Builder
public class MerchantStatsVO {

    /** 商家总数 */
    private Long total;

    /** 营业中 */
    private Long openCount;

    /** 待审核 */
    private Long pendingCount;

    /** 已封禁 */
    private Long suspendedCount;

    /** 已打烊 */
    private Long closedCount;

}
