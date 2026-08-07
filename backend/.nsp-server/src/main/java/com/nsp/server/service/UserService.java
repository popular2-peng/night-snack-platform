package com.nsp.server.service;

import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.RegisterDTO;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.UserVO;

/**
 * 用户业务接口 — 登录、注册、用户信息管理
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册参数
     * @return 登录凭证（含 Token）
     * @throws com.nsp.common.exception.BizException 用户名/手机号已存在
     */
    LoginVO register(RegisterDTO dto);

    /**
     * 用户登录（用户专用，仅查 user 表）
     *
     * @param dto 登录参数（手机号优先，用户名兜底）
     * @return 登录凭证（含 Token）
     * @throws com.nsp.common.exception.BizException 用户不存在或密码错误
     */
    LoginVO login(LoginDTO dto);

    /**
     * 管理员登录（管理员专用，仅查 admin 表）
     *
     * @param dto 登录参数（用户名 + 密码）
     * @return 登录凭证（含 Token）
     * @throws com.nsp.common.exception.BizException 管理员不存在或密码错误
     */
    LoginVO adminLogin(LoginDTO dto);

    /**
     * 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户 VO
     * @throws com.nsp.common.exception.BizException 用户不存在
     */
    UserVO getUserById(Long userId);

}
