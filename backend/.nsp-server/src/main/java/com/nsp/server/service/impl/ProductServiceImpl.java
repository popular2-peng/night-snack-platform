package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.ProductDTO;
import com.nsp.pojo.dto.ProductSpecDTO;
import com.nsp.pojo.entity.Category;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.entity.ProductSpec;
import com.nsp.pojo.vo.ProductVO;
import com.nsp.pojo.vo.SpecVO;
import com.nsp.server.mapper.CategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.ProductMapper;
import com.nsp.server.mapper.ProductSpecMapper;
import com.nsp.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品业务实现
 *
 * <p>⚡ 库存扣减将在 Phase 2 中通过 Redis + Lua 实现，本阶段仅操作 MySQL
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public List<ProductVO> listByMerchant(Long merchantId, Long categoryId, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getUpdatedAt);

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);
        return result.getRecords().stream().map(this::toVO).toList();
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, MessageConst.PRODUCT_NOT_FOUND_MSG);
        }
        return toVO(product);
    }

    @Override
    @Transactional
    public ProductVO create(Long merchantId, ProductDTO dto) {
        // 校验品类属于该商家
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || !category.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND, MessageConst.CATEGORY_NOT_FOUND_MSG);
        }

        Product product = new Product();
        product.setMerchantId(merchantId);
        BeanUtil.copyProperties(dto, product);
        product.setStatus("ON"); // 新建默认上架
        productMapper.insert(product);

        log.info("新增商品: productId={}, merchantId={}, name={}", product.getId(), merchantId, dto.getName());
        return toVO(product);
    }

    @Override
    @Transactional
    public ProductVO update(Long id, Long merchantId, ProductDTO dto) {
        Product product = findOwnProduct(id, merchantId);
        BeanUtil.copyProperties(dto, product, "id", "merchantId");
        productMapper.updateById(product);
        log.info("更新商品: productId={}, name={}", id, dto.getName());
        return toVO(product);
    }

    @Override
    public void updateStatus(Long id, Long merchantId, String status) {
        findOwnProduct(id, merchantId);
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        productMapper.updateById(update);
        log.info("更新商品状态: productId={}, status={}", id, status);
    }

    @Override
    @Transactional
    public void addSpec(Long productId, Long merchantId, ProductSpecDTO dto) {
        findOwnProduct(productId, merchantId);
        ProductSpec spec = new ProductSpec();
        spec.setProductId(productId);
        BeanUtil.copyProperties(dto, spec);
        productSpecMapper.insert(spec);
        log.info("新增规格: specId={}, productId={}, name={}, stock={}", spec.getId(), productId, dto.getName(), dto.getStock());
    }

    @Override
    public void removeSpec(Long specId, Long merchantId) {
        ProductSpec spec = productSpecMapper.selectById(specId);
        if (spec == null) {
            throw new BizException(ErrorCode.SPEC_NOT_FOUND, MessageConst.SPEC_NOT_FOUND_MSG);
        }
        // 校验规格所属商品归该商家所有
        findOwnProduct(spec.getProductId(), merchantId);
        productSpecMapper.deleteById(specId);
        log.info("删除规格: specId={}", specId);
    }

    // ──────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────

    private Product findOwnProduct(Long id, Long merchantId) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, MessageConst.PRODUCT_NOT_FOUND_MSG);
        }
        if (!product.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.PRODUCT_NOT_OWNER_MSG);
        }
        return product;
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtil.copyProperties(product, vo);

        // 品类名称
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // 规格列表
        List<ProductSpec> specs = productSpecMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, product.getId()));
        vo.setSpecs(specs.stream().map(s -> {
            SpecVO sv = new SpecVO();
            BeanUtil.copyProperties(s, sv);
            return sv;
        }).toList());

        return vo;
    }

}
