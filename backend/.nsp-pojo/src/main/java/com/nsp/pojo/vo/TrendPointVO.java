package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 趋势数据点 — 某日计数
 */
@Data
@Builder
public class TrendPointVO {

    /** 日期（MM-dd） */
    private String date;

    /** 当日数量 */
    private Long count;

}
