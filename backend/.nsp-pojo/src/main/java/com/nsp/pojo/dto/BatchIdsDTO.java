package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量操作请求体 — 通用 ID 列表
 */
@Data
public class BatchIdsDTO {

    @NotEmpty(message = "请至少选择一条数据")
    @Size(max = 100, message = "单次批量操作最多 100 条")
    private List<Long> ids;

}
