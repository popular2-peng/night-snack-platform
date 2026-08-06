package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.RegisterDTO;
import com.nsp.pojo.entity.Admin;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.UserVO;
import com.nsp.server.mapper.AdminMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.security.JwtTokenProvider;
import com.nsp.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现 — 登录跨 admin / user / merchant 三表
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final MerchantMapper merchantMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 检查用户名是否在 user 表中已存在
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (usernameCount > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, MessageConst.USERNAME_EXISTS_MSG);
        }

        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, MessageConst.PHONE_EXISTS_MSG);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        log.info("新用户注册: userId={}, username={}", user.getId(), user.getUsername());

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), "USER");

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role("USER")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 手机号登录（用户/商家，注册时填写了手机号）
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            return loginByPhone(dto);
        }
        // 用户名登录（管理员；兼容旧逻辑）
        return loginByUsername(dto);
    }

    /** 手机号 + 密码登录：先查商家表，再查用户表 */
    private LoginVO loginByPhone(LoginDTO dto) {
        // 1) 商家表
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, dto.getPhone()));
        if (merchant != null) {
            if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
                throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
            }
            if ("SUSPENDED".equals(merchant.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_MERCHANT_BANNED_MSG);
            }
            log.info("商家登录(手机号): merchantId={}, phone={}", merchant.getId(), dto.getPhone());
            String accessToken = jwtTokenProvider.createAccessToken(merchant.getId(), "MERCHANT");
            String refreshToken = jwtTokenProvider.createRefreshToken(merchant.getId(), "MERCHANT");
            return LoginVO.builder()
                    .userId(merchant.getId())
                    .username(merchant.getUsername())
                    .role("MERCHANT")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        // 2) 用户表（顾客）
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user != null) {
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
            }
            if ("DISABLED".equals(user.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_ACCOUNT_BANNED_MSG);
            }
            log.info("用户登录(手机号): userId={}, phone={}", user.getId(), dto.getPhone());
            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), "USER");
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), "USER");
            return LoginVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role("USER")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_PHONE_NOT_REGISTERED_MSG);
    }

    /** 用户名 + 密码登录：admin → merchant → user（管理员为主，兼容旧逻辑） */
    private LoginVO loginByUsername(LoginDTO dto) {
        // 1) 尝试 admin 表
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (admin != null) {
            if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
                throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
            }
            if ("DISABLED".equals(admin.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_ACCOUNT_BANNED_MSG);
            }
            log.info("管理员登录: adminId={}, username={}", admin.getId(), admin.getUsername());
            String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), "ADMIN");
            String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId(), "ADMIN");
            return LoginVO.builder()
                    .userId(admin.getId())
                    .username(admin.getUsername())
                    .role("ADMIN")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        // 2) 尝试 merchant 表（旧逻辑兼容）
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUsername, dto.getUsername()));
        if (merchant != null) {
            if (!passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
                throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
            }
            if ("SUSPENDED".equals(merchant.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_MERCHANT_BANNED_MSG);
            }
            log.info("商家登录(用户名): merchantId={}, username={}", merchant.getId(), merchant.getUsername());
            String accessToken = jwtTokenProvider.createAccessToken(merchant.getId(), "MERCHANT");
            String refreshToken = jwtTokenProvider.createRefreshToken(merchant.getId(), "MERCHANT");
            return LoginVO.builder()
                    .userId(merchant.getId())
                    .username(merchant.getUsername())
                    .role("MERCHANT")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        // 3) 尝试 user 表（旧逻辑兼容）
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user != null) {
            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
            }
            if ("DISABLED".equals(user.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.FORBIDDEN_ACCOUNT_BANNED_MSG);
            }
            log.info("用户登录(用户名): userId={}, username={}", user.getId(), user.getUsername());
            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), "USER");
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), "USER");
            return LoginVO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role("USER")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_NOT_FOUND_MSG);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_NOT_FOUND_MSG);
        }
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        vo.setRole("USER");
        return vo;
    }

}
