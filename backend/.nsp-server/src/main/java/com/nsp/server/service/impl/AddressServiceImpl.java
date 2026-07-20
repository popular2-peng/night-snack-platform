package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.AddressDTO;
import com.nsp.pojo.entity.UserAddress;
import com.nsp.pojo.vo.AddressVO;
import com.nsp.server.mapper.AddressMapper;
import com.nsp.server.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址业务实现
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);
    private static final int MAX_ADDRESS_COUNT = 20;

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> listByUserId(Long userId) {
        List<UserAddress> list = addressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public AddressVO getById(Long id, Long userId) {
        UserAddress addr = findOwnAddress(id, userId);
        return toVO(addr);
    }

    @Override
    @Transactional
    public AddressVO create(AddressDTO dto, Long userId) {
        Long count = addressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BizException(ErrorCode.ADDRESS_LIMIT_EXCEEDED,
                    "地址数量已达上限（" + MAX_ADDRESS_COUNT + "）");
        }

        UserAddress addr = new UserAddress();
        addr.setUserId(userId);
        BeanUtil.copyProperties(dto, addr);
        addr.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault());

        if (addr.getIsDefault()) {
            clearDefault(userId);
        }

        addressMapper.insert(addr);
        log.info("新增地址: addrId={}, userId={}", addr.getId(), userId);
        return toVO(addr);
    }

    @Override
    @Transactional
    public AddressVO update(Long id, AddressDTO dto, Long userId) {
        UserAddress addr = findOwnAddress(id, userId);
        BeanUtil.copyProperties(dto, addr, "id", "userId");
        addr.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault());

        if (addr.getIsDefault()) {
            clearDefault(userId);
        }

        addressMapper.updateById(addr);
        log.info("更新地址: addrId={}, userId={}", id, userId);
        return toVO(addr);
    }

    @Override
    public void delete(Long id, Long userId) {
        findOwnAddress(id, userId);
        addressMapper.deleteById(id);
        log.info("删除地址: addrId={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        findOwnAddress(id, userId);
        clearDefault(userId);
        UserAddress addr = new UserAddress();
        addr.setId(id);
        addr.setIsDefault(true);
        addressMapper.updateById(addr);
        log.info("设置默认地址: addrId={}, userId={}", id, userId);
    }

    private UserAddress findOwnAddress(Long id, Long userId) {
        UserAddress addr = addressMapper.selectById(id);
        if (addr == null) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND, "地址不存在");
        }
        if (!addr.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作他人地址");
        }
        return addr;
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null,
                new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getIsDefault, true)
                        .set(UserAddress::getIsDefault, false));
    }

    private AddressVO toVO(UserAddress addr) {
        AddressVO vo = new AddressVO();
        BeanUtil.copyProperties(addr, vo);
        return vo;
    }

}
