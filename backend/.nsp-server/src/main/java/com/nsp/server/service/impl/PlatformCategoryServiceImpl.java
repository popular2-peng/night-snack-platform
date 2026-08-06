package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.CategoryReorderDTO;
import com.nsp.pojo.dto.CategorySaveDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantCategory;
import com.nsp.pojo.entity.PlatformCategory;
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

        // 检查是否有关联商家
        Long merchantCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getCategoryId, id));
        Long junctionCount = merchantCategoryMapper.selectCount(
                new LambdaQueryWrapper<MerchantCategory>()
                        .eq(MerchantCategory::getCategoryId, id));
        if (merchantCount > 0 || junctionCount > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_MERCHANTS, MessageConst.CATEGORY_HAS_MERCHANTS_MSG);
        }

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
