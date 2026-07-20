package com.nsp.server.service;

import com.nsp.pojo.dto.ProductDTO;
import com.nsp.pojo.dto.ProductSpecDTO;
import com.nsp.pojo.vo.ProductVO;

import java.util.List;

/**
 * 商品业务接口
 */
public interface ProductService {

    List<ProductVO> listByMerchant(Long merchantId, Long categoryId, int page, int size);

    ProductVO getById(Long id);

    ProductVO create(Long merchantId, ProductDTO dto);

    ProductVO update(Long id, Long merchantId, ProductDTO dto);

    void updateStatus(Long id, Long merchantId, String status);

    /**
     * 添加规格（本阶段简化为单表操作，不涉及库存 Redis 同步）
     */
    void addSpec(Long productId, Long merchantId, ProductSpecDTO dto);

    void removeSpec(Long specId, Long merchantId);

}
