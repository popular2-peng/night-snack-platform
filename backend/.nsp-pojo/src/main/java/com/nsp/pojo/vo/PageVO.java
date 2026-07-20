package com.nsp.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 VO — 所有列表接口统一结构
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    /** 总记录数 */
    private Long total;

    /** 当前页数据列表 */
    private List<T> list;

}
