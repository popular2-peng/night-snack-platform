package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量操作请求体 — 通用 ID 列表
 */
@Data
public class BatchIdsDTO {

    @NotEmpty(message = "请至少选择一条数据")
    private List<Long> ids;

}
