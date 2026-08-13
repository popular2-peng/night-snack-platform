package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.MerchantShopUpdateDTO;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.security.MerchantContext;
import com.nsp.server.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商家店铺管理接口
 *
 * <p>商家身份由 JWT 解析（sub = merchant.id），不再接收前端传 merchantId。
 */
@Tag(name = "商家店铺", description = "店铺信息、营业状态管理")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "获取店铺信息（当前登录商家）")
    @GetMapping("/shop")
    public Result<MerchantVO> getShop() {
        return Result.ok(merchantService.getById(MerchantContext.currentMerchantId()));
    }

    @Operation(summary = "更新店铺信息（当前登录商家，全字段可选部分更新；地址变更自动重新解析坐标）")
    @PutMapping("/shop")
    public Result<MerchantVO> updateShop(@Valid @RequestBody MerchantShopUpdateDTO dto) {
        return Result.ok(merchantService.updateShop(MerchantContext.currentMerchantId(), dto));
    }

    @Operation(summary = "更新营业状态（营业中/打烊，当前登录商家）")
    @PutMapping("/shop/status")
    public Result<Void> updateStatus(@RequestParam String status) {
        Long merchantId = MerchantContext.currentMerchantId();
        merchantService.updateStatus(merchantId, merchantId, status);
        return Result.ok();
    }

    @Operation(summary = "浏览商家列表（用户端公开接口，按距离排序分页）")
    @GetMapping("/list")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<PageVO<MerchantVO>> listNearby(@RequestParam BigDecimal lat,
                                                 @RequestParam BigDecimal lng,
                                                 @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                 @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(merchantService.listNearby(lat, lng, page, size));
    }

    @Operation(summary = "商家详情")
    @GetMapping("/{id}")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<MerchantVO> getById(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

}
