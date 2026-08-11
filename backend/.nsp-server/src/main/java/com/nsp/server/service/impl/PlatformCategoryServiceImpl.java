package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.CategoryReorderDTO;
import com.nsp.pojo.dto.CategorySaveDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantCategory;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.CategoryVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.MerchantCategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.service.PlatformCategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台品类业务实现
 */
@Service
@RequiredArgsConstructor
public class PlatformCategoryServiceImpl implements PlatformCategoryService {

    private static final Logger log = LoggerFactory.getLogger(PlatformCategoryServiceImpl.class);

    private final PlatformCategoryMapper platformCategoryMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;

    @Override
    public PageVO<CategoryVO> listCategories(int page, int size, String keyword, String status,
                                             String dateStart, String dateEnd) {
        LambdaQueryWrapper<PlatformCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformCategory::getDeleted, 0);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(PlatformCategory::getName, keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(PlatformCategory::getStatus, status.toUpperCase());
        }
        // 创建时间范围
        if (dateStart != null && !dateStart.isBlank()) {
            wrapper.ge(PlatformCategory::getCreatedAt, LocalDateTime.parse(dateStart + "T00:00:00"));
        }
        if (dateEnd != null && !dateEnd.isBlank()) {
            wrapper.le(PlatformCategory::getCreatedAt, LocalDateTime.parse(dateEnd + "T23:59:59"));
        }
        wrapper.orderByAsc(PlatformCategory::getSortOrder)
               .orderByDesc(PlatformCategory::getCreatedAt);

        Page<PlatformCategory> result = platformCategoryMapper.selectPage(new Page<>(page, size), wrapper);
        List<CategoryVO> list = result.getRecords().stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            BeanUtil.copyProperties(c, vo);
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public List<CategoryVO> listAllEnabled() {
        List<PlatformCategory> list = platformCategoryMapper.selectList(
                new LambdaQueryWrapper<PlatformCategory>()
                        .eq(PlatformCategory::getStatus, "ENABLED")
                        .eq(PlatformCategory::getDeleted, 0)
                        .orderByAsc(PlatformCategory::getSortOrder));
        return list.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            BeanUtil.copyProperties(c, vo);
            return vo;
        }).toList();
    }

    @Override
    public Map<String, Long> getStats() {
        List<PlatformCategory> all = platformCategoryMapper.selectList(
                new LambdaQueryWrapper<PlatformCategory>().eq(PlatformCategory::getDeleted, 0));
        long total = all.size();
        long enabled = all.stream().filter(c -> "ENABLED".equals(c.getStatus())).count();
        long disabled = all.stream().filter(c -> "DISABLED".equals(c.getStatus())).count();

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("enabled", enabled);
        stats.put("disabled", disabled);
        return stats;
    }

    @Override
    public CategoryVO getById(Long id) {
        PlatformCategory c = findById(id);
        CategoryVO vo = new CategoryVO();
        BeanUtil.copyProperties(c, vo);
        return vo;
    }

    @Override
    @Transactional
    public CategoryVO create(CategorySaveDTO dto) {
        // 校验名称
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BizException(ErrorCode.CATEGORY_NAME_LENGTH, MessageConst.CATEGORY_NAME_EMPTY_MSG);
        }
        if (dto.getName().length() < 2 || dto.getName().length() > 10) {
            throw new BizException(ErrorCode.CATEGORY_NAME_LENGTH, MessageConst.CATEGORY_NAME_LENGTH_MSG);
        }

        // 校验唯一
        Long count = platformCategoryMapper.selectCount(
                new LambdaQueryWrapper<PlatformCategory>()
                        .eq(PlatformCategory::getName, dto.getName())
                        .eq(PlatformCategory::getDeleted, 0));
        if (count > 0) {
            throw new BizException(ErrorCode.CATEGORY_NAME_EXISTS, MessageConst.CATEGORY_NAME_EXISTS_MSG);
        }

        PlatformCategory category = new PlatformCategory();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setIconType(dto.getIconType() != null ? dto.getIconType() : "EMOJI");

        // sort_order 默认 = 最大值 + 1
        if (dto.getSortOrder() != null) {
            if (dto.getSortOrder() < 0) {
                throw new BizException(ErrorCode.CATEGORY_SORT_INVALID, MessageConst.CATEGORY_SORT_INVALID_MSG);
            }
            category.setSortOrder(dto.getSortOrder());
        } else {
            category.setSortOrder(getMaxSortOrder() + 1);
        }

        category.setStatus(dto.getStatus() != null ? dto.getStatus() : "ENABLED");
        category.setRemark(dto.getRemark());
        platformCategoryMapper.insert(category);

        log.info("新增平台品类: id={}, name={}, sortOrder={}", category.getId(), category.getName(), category.getSortOrder());

        CategoryVO vo = new CategoryVO();
        BeanUtil.copyProperties(category, vo);
        return vo;
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CategorySaveDTO dto) {
        PlatformCategory category = findById(id);

        if (dto.getName() != null) {
            if (dto.getName().length() < 2 || dto.getName().length() > 10) {
                throw new BizException(ErrorCode.CATEGORY_NAME_LENGTH, MessageConst.CATEGORY_NAME_LENGTH_MSG);
            }
            // 校验唯一（排除自身）
            Long count = platformCategoryMapper.selectCount(
                    new LambdaQueryWrapper<PlatformCategory>()
                            .eq(PlatformCategory::getName, dto.getName())
                            .eq(PlatformCategory::getDeleted, 0)
                            .ne(PlatformCategory::getId, id));
            if (count > 0) {
                throw new BizException(ErrorCode.CATEGORY_NAME_EXISTS, MessageConst.CATEGORY_NAME_EXISTS_MSG);
            }
            category.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        if (dto.getIconType() != null) {
            category.setIconType(dto.getIconType());
        }
        if (dto.getSortOrder() != null) {
            if (dto.getSortOrder() < 0) {
                throw new BizException(ErrorCode.CATEGORY_SORT_INVALID, MessageConst.CATEGORY_SORT_INVALID_MSG);
            }
            // 插入式重排：目标位置及其后的品类顺延，避免排序覆盖
            shiftSortOrder(category.getSortOrder(), dto.getSortOrder());
            category.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            category.setRemark(dto.getRemark());
        }

        platformCategoryMapper.updateById(category);
        log.info("修改平台品类: id={}, name={}", category.getId(), category.getName());

        CategoryVO vo = new CategoryVO();
        BeanUtil.copyProperties(category, vo);
        return vo;
    }

    @Override
    @Transactional
    public void enable(Long id) {
        PlatformCategory category = findById(id);
        if ("ENABLED".equals(category.getStatus())) {
            return; // 幂等
        }
        category.setStatus("ENABLED");
        platformCategoryMapper.updateById(category);
        log.info("启用品类: id={}, name={}", id, category.getName());
    }

    @Override
    @Transactional
    public void disable(Long id) {
        PlatformCategory category = findById(id);
        if ("DISABLED".equals(category.getStatus())) {
            return; // 幂等
        }
        // 有商家绑定该品类时禁止停用
        assertNoMerchantBound(id, MessageConst.CATEGORY_HAS_MERCHANTS_DISABLE_MSG);
        category.setStatus("DISABLED");
        platformCategoryMapper.updateById(category);
        log.info("停用品类: id={}, name={}", id, category.getName());
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        PlatformCategory category = findById(id);

        // 必须已停用
        if (!"DISABLED".equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_MUST_DISABLE, MessageConst.CATEGORY_MUST_DISABLE_MSG);
        }

        // 有商家绑定该品类时禁止删除
        assertNoMerchantBound(id, MessageConst.CATEGORY_HAS_MERCHANTS_MSG);

        // 软删
        category.setDeleted(1);
        platformCategoryMapper.updateById(category);
        log.info("软删品类: id={}, name={}", id, category.getName());
    }

    @Override
    @Transactional
    public void reorder(List<CategoryReorderDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (CategoryReorderDTO item : items) {
            if (item.getSortOrder() != null && item.getSortOrder() < 0) {
                throw new BizException(ErrorCode.CATEGORY_SORT_INVALID, MessageConst.CATEGORY_SORT_INVALID_MSG);
            }
            PlatformCategory category = platformCategoryMapper.selectById(item.getId());
            if (category == null || category.getDeleted() == 1) {
                throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, String.format(MessageConst.CATEGORY_NOT_EXISTS_WITH_ID_MSG, item.getId()));
            }
            category.setSortOrder(item.getSortOrder());
            platformCategoryMapper.updateById(category);
        }
        log.info("批量更新品类排序: count={}", items.size());
    }

    // ---- 内部方法 ----

    /**
     * 品类排序插入式重排（配合 update 修改 sortOrder 使用）
     *
     * <p>将当前品类移动到 newSort 位置时，被跨越位置上的品类整体顺延：
     * <ul>
     *   <li>后移（newSort &gt; oldSort）：区间 (oldSort, newSort] 内的品类排序 +1</li>
     *   <li>前移（newSort &lt; oldSort）：区间 [newSort, oldSort) 内的品类排序 +1</li>
     * </ul>
     * 例如把排序 3 改为 4，原排序 4 及之后的品类依次变为 5、6、7...
     */
    private void shiftSortOrder(int oldSort, int newSort) {
        if (oldSort == newSort) {
            return;
        }
        if (newSort > oldSort) {
            platformCategoryMapper.update(null, new LambdaUpdateWrapper<PlatformCategory>()
                    .eq(PlatformCategory::getDeleted, 0)
                    .gt(PlatformCategory::getSortOrder, oldSort)
                    .le(PlatformCategory::getSortOrder, newSort)
                    .setSql("sort_order = sort_order + 1"));
        } else {
            platformCategoryMapper.update(null, new LambdaUpdateWrapper<PlatformCategory>()
                    .eq(PlatformCategory::getDeleted, 0)
                    .ge(PlatformCategory::getSortOrder, newSort)
                    .lt(PlatformCategory::getSortOrder, oldSort)
                    .setSql("sort_order = sort_order + 1"));
        }
    }

    /** 检查品类是否绑定商家（merchant.category_id 或 merchant_category 关联表） */
    private void assertNoMerchantBound(Long id, String message) {
        if (countMerchantBound(id) > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_MERCHANTS, message);
        }
    }

    /** 品类绑定商家数 */
    private long countMerchantBound(Long id) {
        Long merchantCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getCategoryId, id));
        Long junctionCount = merchantCategoryMapper.selectCount(
                new LambdaQueryWrapper<MerchantCategory>()
                        .eq(MerchantCategory::getCategoryId, id));
        return (merchantCount == null ? 0 : merchantCount) + (junctionCount == null ? 0 : junctionCount);
    }

    // ──────────────────────────────────────
    // 批量操作（选中若干条，不满足条件的跳过并汇总）
    // ──────────────────────────────────────

    @Override
    @Transactional
    public BatchResultVO batchEnable(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        int enabled = 0, deleted = 0;
        for (Long id : ids) {
            PlatformCategory category = platformCategoryMapper.selectById(id); // 逻辑删除 → null
            if (category == null) { deleted++; continue; }
            if ("ENABLED".equals(category.getStatus())) { enabled++; continue; }

            category.setStatus("ENABLED");
            platformCategoryMapper.updateById(category);
            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("批量启用品类: id={}", id);
        }
        result.setSkippedCount(enabled + deleted);
        if (enabled > 0) result.addSkipped(enabled + " 个品类已启用");
        if (deleted > 0) result.addSkipped(deleted + " 个品类已删除");
        log.info("批量启用品类: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public BatchResultVO batchDisable(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        int disabled = 0, bound = 0, deleted = 0;
        for (Long id : ids) {
            PlatformCategory category = platformCategoryMapper.selectById(id);
            if (category == null) { deleted++; continue; }
            if ("DISABLED".equals(category.getStatus())) { disabled++; continue; }
            // 有关联商家店铺的品类不可停用
            if (countMerchantBound(id) > 0) { bound++; continue; }

            category.setStatus("DISABLED");
            platformCategoryMapper.updateById(category);
            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("批量停用品类: id={}", id);
        }
        result.setSkippedCount(disabled + bound + deleted);
        if (disabled > 0) result.addSkipped(disabled + " 个品类已停用");
        if (bound > 0) result.addSkipped(bound + " 个品类有关联商家不可停用");
        if (deleted > 0) result.addSkipped(deleted + " 个品类已删除");
        log.info("批量停用品类: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public BatchResultVO batchDelete(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        int notDisabled = 0, bound = 0, deleted = 0;
        for (Long id : ids) {
            PlatformCategory category = platformCategoryMapper.selectById(id);
            if (category == null) { deleted++; continue; }
            // 仅停用状态可删除
            if (!"DISABLED".equals(category.getStatus())) { notDisabled++; continue; }
            // 有关联商家店铺的品类不可删除
            if (countMerchantBound(id) > 0) { bound++; continue; }

            category.setDeleted(1); // 软删
            platformCategoryMapper.updateById(category);
            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("批量删除品类: id={}", id);
        }
        result.setSkippedCount(notDisabled + bound + deleted);
        if (notDisabled > 0) result.addSkipped(notDisabled + " 个品类未停用不可删除");
        if (bound > 0) result.addSkipped(bound + " 个品类有关联商家不可删除");
        if (deleted > 0) result.addSkipped(deleted + " 个品类已删除");
        log.info("批量删除品类: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    private PlatformCategory findById(Long id) {
        PlatformCategory c = platformCategoryMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw new BizException(ErrorCode.CATEGORY_NOT_EXISTS, MessageConst.CATEGORY_NOT_EXISTS_OR_DELETED_MSG);
        }
        return c;
    }

    private int getMaxSortOrder() {
        List<PlatformCategory> list = platformCategoryMapper.selectList(
                new LambdaQueryWrapper<PlatformCategory>()
                        .eq(PlatformCategory::getDeleted, 0)
                        .orderByDesc(PlatformCategory::getSortOrder)
                        .last("LIMIT 1"));
        return list.isEmpty() ? 0 : list.get(0).getSortOrder();
    }

}
