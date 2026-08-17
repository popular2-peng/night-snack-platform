package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.CategoryVO;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.pojo.vo.ProductVO;
import com.nsp.server.service.MerchantService;
import com.nsp.server.service.PlatformCategoryService;
import com.nsp.server.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端浏览接口 — 商家 / 商品 / 分类浏览（匿名可访问，无需登录）
 *
 * <p>与商家后台 {@code /api/merchant/**} 区分：本控制器专供移动端用户浏览公开信息，
 * 复用扁平化的 {@link MerchantService} / {@link ProductService} / {@link PlatformCategoryService}，
 * 不写任何与商家经营相关的写操作（增删改仍在商家端）。
 */
@Tag(name = "用户端浏览", description = "首页商家/商品/分类浏览（匿名）")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class BrowseController {

    private final MerchantService merchantService;
    private final ProductService productService;
    private final PlatformCategoryService platformCategoryService;

    @Operation(summary = "分类列表（平台启用品类）")
    @GetMapping("/categories")
    public Result<List<CategoryVO>> listCategories() {
        return Result.ok(platformCategoryService.listAllEnabled());
    }

    @Operation(summary = "附近商家列表（按距离排序分页）")
    @GetMapping("/merchants")
    public Result<PageVO<MerchantVO>> listMerchants(
            @Parameter(description = "纬度") @RequestParam BigDecimal lat,
            @Parameter(description = "经度") @RequestParam BigDecimal lng,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(merchantService.listNearby(lat, lng, page, size));
    }

    @Operation(summary = "商家详情")
    @GetMapping("/merchants/{id}")
    public Result<MerchantVO> getMerchant(@PathVariable Long id) {
        return Result.ok(merchantService.getById(id));
    }

    @Operation(summary = "商家在售商品列表（仅上架商品）")
    @GetMapping("/merchants/{id}/products")
    public Result<PageVO<ProductVO>> listProducts(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(productService.listByMerchant(id, null, page, size, null, "ON"));
    }

    @Operation(summary = "商品详情（仅上架商品，属于指定商家）")
    @GetMapping("/merchants/{merchantId}/products/{productId}")
    public Result<ProductVO> getProduct(@PathVariable Long merchantId,
                                        @PathVariable Long productId) {
        return Result.ok(productService.getForBrowse(merchantId, productId));
    }

}
