package com.nsp.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果 — 成功数与跳过明细（跳过原因由服务端汇总）
 *
 * <p>前端可直接展示 `message`（如"成功封禁 7 个账号，另外 3 个为已封禁账号"）
 */
@Data
public class BatchResultVO {

    /** 成功处理的条数 */
    private int successCount;

    /** 跳过的条数（不满足操作条件） */
    private int skippedCount;

    /** 跳过原因摘要（已按原因去重，如 "2 个账号已封禁"、"1 个品类有关联商家"） */
    private List<String> skippedReasons = new ArrayList<>();

    public void addSkipped(String reason) {
        skippedReasons.add(reason);
    }

    /** 汇总后的提示文案（前端直接展示） */
    public String getMessage() {
        StringBuilder sb = new StringBuilder("成功处理 ").append(successCount).append(" 项");
        if (!skippedReasons.isEmpty()) {
            sb.append("，跳过 ").append(skippedCount).append(" 项：").append(String.join("、", skippedReasons));
        }
        return sb.toString();
    }

}
