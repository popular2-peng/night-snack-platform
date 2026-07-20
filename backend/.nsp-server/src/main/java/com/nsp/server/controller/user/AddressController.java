package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.AddressDTO;
import com.nsp.pojo.vo.AddressVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址管理接口
 */
@Tag(name = "收货地址", description = "用户收货地址 CRUD")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
@RequireRole("USER")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "获取地址列表")
    @GetMapping
    public Result<List<AddressVO>> list(@AuthenticationPrincipal Long userId) {
        return Result.ok(addressService.listByUserId(userId));
    }

    @Operation(summary = "获取单个地址")
    @GetMapping("/{id}")
    public Result<AddressVO> getById(@PathVariable Long id,
                                     @AuthenticationPrincipal Long userId) {
        return Result.ok(addressService.getById(id, userId));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressDTO dto,
                                    @AuthenticationPrincipal Long userId) {
        return Result.ok(addressService.create(dto, userId));
    }

    @Operation(summary = "更新地址")
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id,
                                    @Valid @RequestBody AddressDTO dto,
                                    @AuthenticationPrincipal Long userId) {
        return Result.ok(addressService.update(id, dto, userId));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal Long userId) {
        addressService.delete(id, userId);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id,
                                   @AuthenticationPrincipal Long userId) {
        addressService.setDefault(id, userId);
        return Result.ok();
    }

}
