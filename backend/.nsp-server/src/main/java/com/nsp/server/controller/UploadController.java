package com.nsp.server.controller;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.UploadBindDTO;
import com.nsp.pojo.vo.UploadSignVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 文件上传接口 — OSS 服务端签名 + 前端直传
 */
@Tag(name = "文件上传", description = "OSS 上传签名、绑定、回调")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final UploadService uploadService;

    @Operation(summary = "获取 OSS 上传签名")
    @GetMapping("/sign")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<UploadSignVO> sign(@RequestParam String bizType,
                                     @RequestParam(required = false) Long targetId,
                                     @RequestParam String fileName,
                                     @AuthenticationPrincipal Long userId) {
        return Result.ok(uploadService.sign(userId, bizType, targetId, fileName));
    }

    @Operation(summary = "绑定上传文件（前端直传成功后回写）")
    @PostMapping("/bind")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<Void> bind(@Valid @RequestBody UploadBindDTO dto,
                             @AuthenticationPrincipal Long userId) {
        uploadService.bind(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "获取文件临时访问 URL")
    @GetMapping("/url")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<String> getUrl(@RequestParam String ossKey) {
        return Result.ok(uploadService.getAccessUrl(ossKey));
    }

    @Operation(summary = "OSS 上传完成回调（匿名，白名单 IP）")
    @PostMapping("/callback")
    public String callback(@RequestParam String ossKey,
                           @RequestParam String bizType,
                           @RequestParam(required = false) Long targetId,
                           @RequestParam Long uploaderId) {
        log.info("OSS 回调: ossKey={}, bizType={}", ossKey, bizType);
        try {
            uploadService.handleCallback(ossKey, bizType, targetId, uploaderId);
            return "success";
        } catch (Exception e) {
            log.error("OSS 回调处理失败", e);
            return "fail";
        }
    }

}
