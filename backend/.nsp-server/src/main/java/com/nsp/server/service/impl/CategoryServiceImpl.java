package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.entity.Category;
import com.nsp.server.mapper.CategoryMapper;
import com.nsp.server.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 品类业务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> listByMerchant(Long merchantId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getMerchantId, merchantId)
                        .orderByAsc(Category::getSort));
    }

    @Override
    public Category create(Long merchantId, String name, Integer sort) {
        Category category = new Category();
        category.setMerchantId(merchantId);
        category.setName(name);
        category.setSort(sort != null ? sort : 0);
        categoryMapper.insert(category);
        log.info("新增品类: id={}, merchantId={}, name={}", category.getId(), merchantId, name);
        return category;
    }

    @Override
    public Category update(Long id, Long merchantId, String name, Integer sort) {
        Category category = categoryMapper.selectById(id);
        if (category == null || !category.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND, MessageConst.CATEGORY_NOT_FOUND_MSG);
        }
        if (name != null) category.setName(name);
        if (sort != null) category.setSort(sort);
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void delete(Long id, Long merchantId) {
        Category category = categoryMapper.selectById(id);
        if (category == null || !category.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND, MessageConst.CATEGORY_NOT_FOUND_MSG);
        }
        categoryMapper.deleteById(id);
        log.info("删除品类: id={}, merchantId={}", id, merchantId);
    }

}
