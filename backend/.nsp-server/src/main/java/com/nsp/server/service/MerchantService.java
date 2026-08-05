package com.nsp.server.service;

import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.MerchantRegisterDTO;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.MerchantVO;

import java.util.List;

/**
 * 商家业务接口
 */
public interface MerchantService {

    /**
     * 查询附近商家（按距离排序）
     *
     * @param lat 用户纬度
     * @param lng 用户经度
     * @return 商家列表（含距离）
     */
    List<MerchantVO> listNearby(java.math.BigDecimal lat, java.math.BigDecimal lng);

    /**
     * 查询商家详情
     */
    MerchantVO getById(Long id);

    /**
     * 更新营业状态
     */
    void updateStatus(Long merchantId, Long userId, String status);

    /**
     * 商家入驻申请（注册）
     *
     * <p>创建申请人 user 账号 + merchant_application 申请记录（PENDING）。
     * 申请中的账号密码即为审核通过后商家登录凭据。
     */
    void register(MerchantRegisterDTO dto);

    /**
     * 商家登录（仅查 merchant 表，手机号 + 密码）
     *
     * @return MERCHANT 角色 LoginVO
     */
    LoginVO login(LoginDTO dto);

}
