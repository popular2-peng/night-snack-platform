package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理业务实现
 */
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementServiceImpl.class);

    private final UserMapper userMapper;

    @Override
    public PageVO<AdminUserVO> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                            .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserVO> list = result.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtil.copyProperties(u, vo);
            vo.setRole("USER");
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public void disableUser(Long id) {
        User user = findById(id);
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "用户已被封禁");
        }
        user.setStatus("DISABLED");
        userMapper.updateById(user);
        log.info("管理员封禁用户: userId={}", id);
    }

    @Override
    public void enableUser(Long id) {
        User user = findById(id);
        if ("ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "用户未被封禁");
        }
        user.setStatus("ACTIVE");
        userMapper.updateById(user);
        log.info("管理员解封用户: userId={}", id);
    }

    private User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        return user;
    }

}
