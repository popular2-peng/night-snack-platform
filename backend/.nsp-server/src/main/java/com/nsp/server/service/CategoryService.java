package com.nsp.server.service;

import com.nsp.pojo.entity.Category;
import java.util.List;

/**
 * 品类业务接口
 */
public interface CategoryService {

    List<Category> listByMerchant(Long merchantId);

    Category create(Long merchantId, String name, Integer sort);

    Category update(Long id, Long merchantId, String name, Integer sort);

    void delete(Long id, Long merchantId);

}
