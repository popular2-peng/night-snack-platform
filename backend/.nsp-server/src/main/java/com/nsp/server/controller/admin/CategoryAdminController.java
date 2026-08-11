package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.BatchIdsDTO;
import com.nsp.pojo.dto.CategoryReorderDTO;
import com.nsp.pojo.dto.CategorySaveDTO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.CategoryVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.PlatformCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台管理后台 — 品类管理（平台级商家分类）
 */
@Tag(name = "品类管理", description = "管理员：品类 CRUD、排序、启用/停用、软删")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class CategoryAdminController {

    private final PlatformCategoryService platformCategoryService;

    @Operation(summary = "品类列表（分页+搜索+状态+日期范围筛选）")
    @GetMapping("/category")
    public Result<PageVO<CategoryVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词（模糊匹配品类名）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：ENABLED / DISABLED") @RequestParam(required = false) String status,
            @Parameter(description = "创建起始日期 yyyy-MM-dd") @RequestParam(required = false) String dateStart,
            @Parameter(description = "创建结束日期 yyyy-MM-dd") @RequestParam(required = false) String dateEnd) {
        return Result.ok(platformCategoryService.listCategories(page, size, keyword, status, dateStart, dateEnd));
    }

    @Operation(summary = "全部启用品类（下拉框用，无分页）")
    @GetMapping("/category/all")
    public Result<List<CategoryVO>> listAllEnabled() {
        return Result.ok(platformCategoryService.listAllEnabled());
    }

    @Operation(summary = "品类统计（总数/启用/停用）")
    @GetMapping("/category/stats")
    public Result<Map<String, Long>> stats() {
        return Result.ok(platformCategoryService.getStats());
    }

    @Operation(summary = "品类详情")
    @GetMapping("/category/{id}")
    public Result<CategoryVO> detail(@Parameter(description = "品类 ID") @PathVariable Long id) {
        return Result.ok(platformCategoryService.getById(id));
    }

    @Operation(summary = "新增品类")
    @PostMapping("/category")
    public Result<CategoryVO> create(@RequestBody CategorySaveDTO dto) {
        return Result.ok(platformCategoryService.create(dto));
    }

    @Operation(summary = "修改品类")
    @PutMapping("/category/{id}")
    public Result<CategoryVO> update(@Parameter(description = "品类 ID") @PathVariable Long id,
                                      @RequestBody CategorySaveDTO dto) {
        return Result.ok(platformCategoryService.update(id, dto));
    }

    @Operation(summary = "启用品类")
    @PutMapping("/category/{id}/enable")
    public Result<Void> enable(@Parameter(description = "品类 ID") @PathVariable Long id) {
        platformCategoryService.enable(id);
        return Result.ok();
    }

    @Operation(summary = "停用品类")
    @PutMapping("/category/{id}/disable")
    public Result<Void> disable(@Parameter(description = "品类 ID") @PathVariable Long id) {
        platformCategoryService.disable(id);
        return Result.ok();
    }

    @Operation(summary = "删除品类（仅停用状态且无关联商家可删）")
    @DeleteMapping("/category/{id}")
    public Result<Void> delete(@Parameter(description = "品类 ID") @PathVariable Long id) {
        platformCategoryService.softDelete(id);
        return Result.ok();
    }

    @Operation(summary = "批量启用品类（已启用/已删除的自动跳过）")
    @PostMapping("/category/batch-enable")
    public Result<BatchResultVO> batchEnable(@RequestBody BatchIdsDTO dto) {
        return Result.ok(platformCategoryService.batchEnable(dto.getIds()));
    }

    @Operation(summary = "批量停用品类（有关联商家店铺的自动跳过）")
    @PostMapping("/category/batch-disable")
    public Result<BatchResultVO> batchDisable(@RequestBody BatchIdsDTO dto) {
        return Result.ok(platformCategoryService.batchDisable(dto.getIds()));
    }

    @Operation(summary = "批量删除品类（仅停用且无关联商家可删，其余跳过）")
    @PostMapping("/category/batch-delete")
    public Result<BatchResultVO> batchDelete(@RequestBody BatchIdsDTO dto) {
        return Result.ok(platformCategoryService.batchDelete(dto.getIds()));
    }

    @Operation(summary = "批量更新品类排序")
    @PostMapping("/category/reorder")
    public Result<Void> reorder(@RequestBody List<CategoryReorderDTO> items) {
        platformCategoryService.reorder(items);
        return Result.ok();
    }

}
