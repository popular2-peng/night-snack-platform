package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商家店铺管理接口
 */
@Tag(name = "商家店铺", description = "店铺信息、营业状态管理")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "获取店铺信息")
    @GetMapping("/shop")
    public Result<MerchantVO> getShop(@AuthenticationPrincipal Long userId) {
        // TODO: 通过 userId 查 merchant，本阶段先通过路径参数传 merchantId
        return Result.ok(null);
    }

    @Operation(summary = "更新营业状态（营业中/打烊）")
    @PutMapping("/shop/status")
    public Result<Void> updateStatus(@RequestParam Long merchantId,
                                     @RequestParam String status,
                                     @AuthenticationPrincipal Long userId) {
        merchantService.updateStatus(merchantId, userId, status);
        return Result.ok();
    }

    @Operation(summary = "浏览商家列表（用户端公开接口）")
    @GetMapping("/list")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<java.util.List<MerchantVO>> listNearby(@RequestParam BigDecimal lat,
                                                          @RequestParam BigDecimal lng) {
        return Result.ok(merchantService.listNearby(lat, lng));
    }

    @Operation(summary = "商家详情")
    @GetMapping("/{id}")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<MerchantVO> getById(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

}
