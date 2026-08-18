package com.nsp.server.service;

import com.nsp.pojo.dto.ProductDTO;
import com.nsp.pojo.dto.ProductSpecDTO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.pojo.vo.ProductVO;

/**
 * 商品业务接口
 */
public interface ProductService {

    PageVO<ProductVO> listByMerchant(Long merchantId, Long categoryId, int page, int size,
                                     String keyword, String status);

    ProductVO getById(Long id);

    /** 用户端浏览商品详情：校验商品属于指定商家且为上架状态 */
    ProductVO getForBrowse(Long merchantId, Long productId);

    /**
     * 商品搜索（跨商家）：按商品名模糊搜索，仅返回上架商品，
     * 且所属商家必须为营业中（搜索页使用）。
     *
     * @param keyword 商品名关键词（模糊匹配）
     * @param page    页码
     * @param size    每页条数
     */
    PageVO<ProductVO> searchOnSale(String keyword, int page, int size);

    ProductVO create(Long merchantId, ProductDTO dto);

    ProductVO update(Long id, Long merchantId, ProductDTO dto);

    /** 删除商品（软删 + 级联删规格）：仅停售状态可删除 */
    void deleteProduct(Long id, Long merchantId);

    void updateStatus(Long id, Long merchantId, String status);

    /**
     * 添加规格（本阶段简化为单表操作，不涉及库存 Redis 同步）
     */
    void addSpec(Long productId, Long merchantId, ProductSpecDTO dto);

    void removeSpec(Long specId, Long merchantId);

}
