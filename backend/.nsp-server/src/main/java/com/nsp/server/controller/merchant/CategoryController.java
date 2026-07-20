package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.entity.Category;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 品类管理接口（商家后台）
 */
@Tag(name = "品类管理", description = "商家管理自己的商品品类")
@RestController
@RequestMapping("/api/merchant/category")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "品类列表")
    @GetMapping
    public Result<List<Category>> list(@RequestParam Long merchantId) {
        return Result.ok(categoryService.listByMerchant(merchantId));
    }

    @Operation(summary = "新增品类")
    @PostMapping
    public Result<Category> create(@RequestParam Long merchantId,
                                   @RequestParam String name,
                                   @RequestParam(required = false, defaultValue = "0") Integer sort) {
        return Result.ok(categoryService.create(merchantId, name, sort));
    }

    @Operation(summary = "更新品类")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id,
                                   @RequestParam Long merchantId,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Integer sort) {
        return Result.ok(categoryService.update(id, merchantId, name, sort));
    }

    @Operation(summary = "删除品类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam Long merchantId) {
        categoryService.delete(id, merchantId);
        return Result.ok();
    }

}
