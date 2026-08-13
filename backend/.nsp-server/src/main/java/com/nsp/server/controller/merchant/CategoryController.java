package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.entity.Category;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.security.MerchantContext;
import com.nsp.server.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 品类管理接口（商家后台）
 *
 * <p>商家身份由 JWT 解析（sub = merchant.id），不再接收前端传 merchantId，
 * 从源头杜绝「商家 A 传商家 B 的 merchantId 查看/操作对方品类」的越权问题。
 */
@Tag(name = "品类管理", description = "商家管理自己的商品品类")
@RestController
@RequestMapping("/api/merchant/category")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "品类列表（当前登录商家）")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listByMerchant(MerchantContext.currentMerchantId()));
    }

    @Operation(summary = "新增品类（归属当前登录商家）")
    @PostMapping
    public Result<Category> create(@RequestParam String name,
                                   @RequestParam(required = false, defaultValue = "0") Integer sort) {
        return Result.ok(categoryService.create(MerchantContext.currentMerchantId(), name, sort));
    }

    @Operation(summary = "更新品类（仅限本店品类）")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Integer sort) {
        return Result.ok(categoryService.update(id, MerchantContext.currentMerchantId(), name, sort));
    }

    @Operation(summary = "删除品类（仅限本店品类）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id, MerchantContext.currentMerchantId());
        return Result.ok();
    }

}
