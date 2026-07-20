package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.server.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理后台接口 — 占位 Controller（Phase 2 实现审核、封禁功能）
 */
@Tag(name = "平台管理", description = "管理员审核、封禁、品类管理")
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {

    @Operation(summary = "管理后台首页（占位）")
    @GetMapping("/dashboard")
    public Result<String> dashboard() {
        return Result.ok("管理后台 — Phase 2 实现");
    }

}
