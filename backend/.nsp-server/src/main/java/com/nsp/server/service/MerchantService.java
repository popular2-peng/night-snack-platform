package com.nsp.server.service;

import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.MerchantRegisterDTO;
import com.nsp.pojo.dto.MerchantShopUpdateDTO;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.pojo.vo.MerchantVO;
import com.nsp.pojo.vo.PageVO;

/**
 * 商家业务接口
 */
public interface MerchantService {

    /**
     * 查询附近商家（按距离升序排序后分页返回）
     *
     * @param lat  用户纬度
     * @param lng  用户经度
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 分页商家列表（含距离）
     */
    PageVO<MerchantVO> listNearby(java.math.BigDecimal lat, java.math.BigDecimal lng, int page, int size);

    /**
     * 全部营业中商家列表（分页，无坐标排序，按入驻时间倒序；可选按主营品类筛选）
     *
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @param categoryId 主营品类 ID（null 表示不筛选）
     * @return 分页商家列表（不含距离）
     */
    PageVO<MerchantVO> listAll(int page, int size, Long categoryId);

    /**
     * 查询商家详情
     */
    MerchantVO getById(Long id);

    /**
     * 更新营业状态
     */
    void updateStatus(Long merchantId, Long userId, String status);

    /**
     * 更新店铺信息（部分更新，仅更新非空字段）
     *
     * <p>地址变更时服务端调用百度地理编码重新解析坐标（失败保留原坐标并告警）；
     * 营业开始/结束时间必须成对填写；更新后返回最新店铺信息。
     *
     * @param merchantId 当前登录商家 ID（JWT 解析）
     * @param dto        更新字段（全可选）
     * @return 更新后的店铺信息
     */
    MerchantVO updateShop(Long merchantId, MerchantShopUpdateDTO dto);

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
