package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.MerchantRegisterDTO;
import com.nsp.pojo.vo.CategoryVO;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.server.service.MerchantService;
import com.nsp.server.service.PlatformCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家端认证 — 登录、入驻申请（注册）、注册页品类下拉
 */
@Tag(name = "商家认证", description = "商家登录、入驻申请、注册页品类列表")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantService merchantService;
    private final PlatformCategoryService platformCategoryService;

    @Operation(summary = "商家登录（手机号 + 密码）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(merchantService.login(dto));
    }

    @Operation(summary = "商家入驻申请（注册）")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody MerchantRegisterDTO dto) {
        merchantService.register(dto);
        return Result.ok();
    }

    @Operation(summary = "平台启用品类（注册页下拉，公开）")
    @GetMapping("/category/all")
    public Result<List<CategoryVO>> listEnabledCategories() {
        return Result.ok(platformCategoryService.listAllEnabled());
    }

}
