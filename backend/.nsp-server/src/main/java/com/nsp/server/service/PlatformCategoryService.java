package com.nsp.server.service;

import com.nsp.pojo.dto.CategoryReorderDTO;
import com.nsp.pojo.dto.CategorySaveDTO;
import com.nsp.pojo.vo.CategoryVO;
import com.nsp.pojo.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 平台品类业务接口 — 对应 category 表（平台级商家分类）
 */
public interface PlatformCategoryService {

    /** 品类列表（分页 + 搜索 + 状态筛选 + 日期范围） */
    PageVO<CategoryVO> listCategories(int page, int size, String keyword, String status,
                                      String dateStart, String dateEnd);

    /** 全部启用品类（下拉框用，无分页） */
    List<CategoryVO> listAllEnabled();

    /** 统计：总数 / 启用 / 停用 */
    Map<String, Long> getStats();

    /** 品类详情 */
    CategoryVO getById(Long id);

    /** 新增品类 */
    CategoryVO create(CategorySaveDTO dto);

    /** 修改品类 */
    CategoryVO update(Long id, CategorySaveDTO dto);

    /** 启用 */
    void enable(Long id);

    /** 停用 */
    void disable(Long id);

    /** 软删（仅停用状态且无关联商家时可删） */
    void softDelete(Long id);

    /** 批量更新排序 */
    void reorder(List<CategoryReorderDTO> items);

}
