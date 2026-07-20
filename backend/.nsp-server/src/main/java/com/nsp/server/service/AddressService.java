package com.nsp.server.service;

import com.nsp.pojo.dto.AddressDTO;
import com.nsp.pojo.vo.AddressVO;

import java.util.List;

/**
 * 用户地址业务接口
 */
public interface AddressService {

    /**
     * 查询用户所有地址
     */
    List<AddressVO> listByUserId(Long userId);

    /**
     * 查询单个地址
     */
    AddressVO getById(Long id, Long userId);

    /**
     * 新增地址
     */
    AddressVO create(AddressDTO dto, Long userId);

    /**
     * 更新地址
     */
    AddressVO update(Long id, AddressDTO dto, Long userId);

    /**
     * 删除地址
     */
    void delete(Long id, Long userId);

    /**
     * 设为默认地址
     */
    void setDefault(Long id, Long userId);

}
