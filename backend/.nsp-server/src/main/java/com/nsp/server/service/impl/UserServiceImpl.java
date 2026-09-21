package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.RegisterDTO;
import com.nsp.pojo.entity.Admin;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.UserVO;
import com.nsp.server.mapper.AdminMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.security.JwtTokenProvider;
import com.nsp.server.service.UserService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 用户名由系统自动生成：「普通用户」+ UUID 前 8 位（保证全局唯一），前端无需填写账号
        String username = "普通用户" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS, MessageConst.PHONE_EXISTS_MSG);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setStatus("ACTIVE");
        // 注册即登录：写入首次登录时间，保证管理端「最后登录」有值
        user.setLastLoginAt(LocalDateTime.now());
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
        // 用户专用登录：仅支持手机号 + 密码（注册时填写了手机号），不支持用户名。
        // 商家走 /api/merchant/login，管理员走 /api/admin/login
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.USER_LOGIN_REQUIRE_PHONE_MSG);
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.USER_PHONE_NOT_REGISTERED_MSG);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_ALREADY_BANNED, MessageConst.USER_BANNED_MSG);
        }
        log.info("用户登录: userId={}, phone={}", user.getId(), user.getPhone());
        updateLastLogin(user.getId());
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

    /** 管理员专用登录：仅查 admin 表 */
    @Override
    public LoginVO adminLogin(LoginDTO dto) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (admin == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.ADMIN_NOT_FOUND_MSG);
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, MessageConst.PASSWORD_ERROR_MSG);
        }
        if ("DISABLED".equals(admin.getStatus())) {
            throw new BizException(ErrorCode.USER_ALREADY_BANNED, MessageConst.USER_BANNED_MSG);
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

    /**
     * 用户登录成功后更新最后登录时间（last_login_at）
     *
     * <p>显式 `updated_at = updated_at` 阻止 MySQL ON UPDATE CURRENT_TIMESTAMP 自动刷新，
     * 避免「最后登录时间」与「修改时间」变成同一时刻（修改时间仅应在资料修改时变化）。
     */
    private void updateLastLogin(Long userId) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getLastLoginAt, LocalDateTime.now())
                .setSql("updated_at = updated_at"));
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
