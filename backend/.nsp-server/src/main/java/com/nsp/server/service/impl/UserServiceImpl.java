package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.enums.UserRole;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.RegisterDTO;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.UserVO;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.security.JwtTokenProvider;
import com.nsp.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO register(RegisterDTO dto) {
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (usernameCount > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, "手机号已注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(UserRole.USER);
        userMapper.insert(user);

        log.info("新用户注册: userId={}, username={}", user.getId(), user.getUsername());

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER.getCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), UserRole.USER.getCode());

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(UserRole.USER.getCode())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, "密码错误");
        }

        log.info("用户登录: userId={}, username={}", user.getId(), user.getUsername());

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().getCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole().getCode());

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getCode())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        vo.setRole(user.getRole().getCode());
        return vo;
    }

}
