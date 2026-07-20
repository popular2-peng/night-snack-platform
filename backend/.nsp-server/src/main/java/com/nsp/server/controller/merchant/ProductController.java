package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.ProductDTO;
import com.nsp.pojo.dto.ProductSpecDTO;
import com.nsp.pojo.vo.ProductVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理接口（商家后台 + 用户浏览）
 */
@Tag(name = "商品管理", description = "商品 CRUD、上架/下架、规格管理")
@RestController
@RequestMapping("/api/merchant/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品列表（用户端也可调用）")
    @GetMapping("/list")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<List<ProductVO>> list(@RequestParam Long merchantId,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Result.ok(productService.listByMerchant(merchantId, categoryId, page, size));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    @RequireRole({"USER", "MERCHANT", "ADMIN"})
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    @RequireRole("MERCHANT")
    public Result<ProductVO> create(@RequestParam Long merchantId,
                                    @Valid @RequestBody ProductDTO dto) {
        return Result.ok(productService.create(merchantId, dto));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    @RequireRole("MERCHANT")
    public Result<ProductVO> update(@PathVariable Long id,
                                    @RequestParam Long merchantId,
                                    @Valid @RequestBody ProductDTO dto) {
        return Result.ok(productService.update(id, merchantId, dto));
    }

    @Operation(summary = "上架/下架")
    @PutMapping("/{id}/status")
    @RequireRole("MERCHANT")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam Long merchantId,
                                     @RequestParam String status) {
        productService.updateStatus(id, merchantId, status);
        return Result.ok();
    }

    @Operation(summary = "添加规格")
    @PostMapping("/{productId}/spec")
    @RequireRole("MERCHANT")
    public Result<Void> addSpec(@PathVariable Long productId,
                                @RequestParam Long merchantId,
                                @Valid @RequestBody ProductSpecDTO dto) {
        productService.addSpec(productId, merchantId, dto);
        return Result.ok();
    }

    @Operation(summary = "删除规格")
    @DeleteMapping("/spec/{specId}")
    @RequireRole("MERCHANT")
    public Result<Void> removeSpec(@PathVariable Long specId,
                                   @RequestParam Long merchantId) {
        productService.removeSpec(specId, merchantId);
        return Result.ok();
    }

}
