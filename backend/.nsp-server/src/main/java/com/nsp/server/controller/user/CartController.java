package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.CartItemDTO;
import com.nsp.pojo.vo.CartVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车接口 — Redis Hash 存储
 */
@Tag(name = "购物车", description = "添加/修改/删除购物车商品，查询购物车")
@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
@RequireRole("USER")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "查询购物车")
    @GetMapping
    public Result<CartVO> getCart(@AuthenticationPrincipal Long userId) {
        return Result.ok(cartService.getCart(userId));
    }

    @Operation(summary = "添加商品到购物车")
    @PostMapping
    public Result<Void> addItem(@Valid @RequestBody CartItemDTO dto,
                                @AuthenticationPrincipal Long userId,
                                @RequestParam Long merchantId) {
        cartService.addItem(userId, dto, merchantId);
        return Result.ok();
    }

    @Operation(summary = "更新商品数量")
    @PutMapping
    public Result<Void> updateQuantity(@Valid @RequestBody CartItemDTO dto,
                                       @AuthenticationPrincipal Long userId) {
        cartService.updateQuantity(userId, dto.getSpecId(), dto.getQuantity());
        return Result.ok();
    }

    @Operation(summary = "移除购物车商品")
    @DeleteMapping("/{specId}")
    public Result<Void> removeItem(@PathVariable Long specId,
                                   @AuthenticationPrincipal Long userId) {
        cartService.removeItem(userId, specId);
        return Result.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clearCart(@AuthenticationPrincipal Long userId) {
        cartService.clearCart(userId);
        return Result.ok();
    }

}
