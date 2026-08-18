package com.nsp.common.constant;

/**
 * 业务错误码常量 — 按域分段，便于 pytest 断言具体业务错误
 *
 * <p>分段规则：
 * <ul>
 *   <li>{@code 0} — 成功</li>
 *   <li>{@code 1000 ~ 1999} — 通用错误（鉴权、参数校验、系统）</li>
 *   <li>{@code 2000 ~ 2999} — 用户域（注册、登录、地址、封禁管理）</li>
 *   <li>{@code 3000 ~ 3999} — 商品/商家域</li>
 *   <li>{@code 4000 ~ 4999} — 订单域</li>
 *   <li>{@code 5000 ~ 5999} — 支付域</li>
 *   <li>{@code 6000 ~ 6999} — 上传域</li>
 *   <li>{@code 7000 ~ 7999} — 商家管理域</li>
 *   <li>{@code 8000 ~ 8999} — 品类管理域</li>
 * </ul>
 */
public final class ErrorCode {

    private ErrorCode() { /* 工具类禁止实例化 */ }

    // ==================== 成功 ====================
    public static final int SUCCESS = 0;

    // ==================== 通用 1000 ~ 1999 ====================
    public static final int UNAUTHORIZED           = 1001; // 未登录
    public static final int FORBIDDEN              = 1002; // 无权限
    public static final int TOKEN_EXPIRED          = 1003; // Token 已过期
    public static final int TOKEN_INVALID          = 1004; // Token 无效
    public static final int PARAM_ERROR            = 1100; // 参数校验失败
    public static final int SYSTEM_ERROR           = 1999; // 系统内部错误

    // ==================== 用户域 2000 ~ 2999 ====================
    public static final int PHONE_EXISTS           = 2001; // 手机号已注册
    public static final int USER_NOT_FOUND         = 2003; // 用户不存在
    public static final int PASSWORD_ERROR         = 2004; // 密码错误
    public static final int ADDRESS_NOT_FOUND      = 2101; // 地址不存在
    public static final int ADDRESS_LIMIT_EXCEEDED  = 2102; // 地址数量超限
    public static final int CART_ITEM_LIMIT         = 2201; // 购物车商品数量超限
    public static final int USER_NOT_FOUND_OR_DELETED = 2301; // 用户不存在或已被删除
    public static final int USER_BAN_REASON_EMPTY    = 2302; // 封禁原因不能为空
    public static final int USER_ALREADY_BANNED      = 2303; // 用户已被封禁，无需重复操作
    public static final int USER_NOT_BANNED          = 2304; // 用户未被封禁，无法解封
    public static final int USER_RESET_PASSWORD_FAIL = 2305; // 重置密码失败（用户状态异常）
    public static final int USER_ACTIVE_CANNOT_DELETE = 2306; // 启用账号不可删除
    public static final int MERCHANT_MUST_BANNED       = 2307; // 商家未封禁不可删除
    public static final int USER_PHONE_FORMAT_ERROR  = 2308; // 手机号格式不正确
    public static final int USER_BAN_DURATION_INVALID = 2309; // 封禁时长不能为负数

    // ==================== 商品/商家域 3000 ~ 3999 ====================
    public static final int MERCHANT_NOT_FOUND     = 3001; // 商家不存在
    public static final int MERCHANT_CLOSED        = 3002; // 商家已打烊
    public static final int MERCHANT_NOT_APPROVED  = 3003; // 商家未通过审核
    public static final int PRODUCT_NOT_FOUND      = 3101; // 商品不存在
    public static final int PRODUCT_OFF_SHELF      = 3102; // 商品已下架
    public static final int SPEC_NOT_FOUND         = 3103; // 规格不存在
    public static final int STOCK_INSUFFICIENT     = 3104; // 库存不足
    public static final int PRODUCT_MUST_OFF_FIRST = 3105; // 商品在售，请先停售再删除
    public static final int CATEGORY_NOT_FOUND     = 3201; // 品类不存在
    public static final int NOT_MERCHANT_OWNER     = 3301; // 非本店商家，无权操作

    // ==================== 订单域 4000 ~ 4999 ====================
    public static final int ORDER_NOT_FOUND        = 4001; // 订单不存在
    public static final int ORDER_STATUS_ERROR     = 4002; // 订单状态不允许此操作
    public static final int OUT_OF_DELIVERY_RANGE  = 4003; // 超出配送范围
    public static final int NOT_IN_BUSINESS_HOURS  = 4004; // 非营业时间
    public static final int CART_EMPTY             = 4005; // 购物车为空
    public static final int ORDER_CREATING         = 4006; // 订单创建中，请勿重复提交

    // ==================== 支付域 5000 ~ 5999 ====================
    public static final int PAY_ORDER_NOT_FOUND    = 5001; // 支付订单不存在
    public static final int PAY_ORDER_PAID         = 5002; // 订单已支付（幂等）
    public static final int PAY_SIGN_ERROR         = 5003; // 支付签名校验失败
    public static final int PAY_CHANNEL_ERROR      = 5004; // 支付渠道异常
    public static final int REFUND_RECORD_NOT_FOUND = 5005; // 订单无成功支付记录，无法退款
    public static final int REFUND_TRADE_NOT_EXIST  = 5006; // 渠道无真实交易（内部降级处理，不对外暴露）
    public static final int REFUND_FAILED           = 5007; // 退款调用失败，可重试

    // ==================== 上传域 6000 ~ 6999 ====================
    public static final int UPLOAD_SIGN_LIMIT      = 6001; // 上传签名请求过于频繁
    public static final int UPLOAD_TYPE_INVALID    = 6002; // 文件类型不允许
    public static final int UPLOAD_SIZE_EXCEED      = 6003; // 文件大小超限
    public static final int UPLOAD_CALLBACK_ERROR  = 6004; // OSS 回调校验失败

    // ==================== 商家管理域 7000 ~ 7999 ====================
    public static final int REJECT_REASON_EMPTY    = 7001; // 拒绝原因不能为空
    public static final int BAN_REASON_EMPTY       = 7002; // 封禁原因不能为空
    public static final int MERCHANT_ALREADY_BANNED = 7003; // 商家已被封禁，无需重复操作
    public static final int MERCHANT_NOT_BANNED    = 7004; // 商家未被封禁，无法解封
    public static final int LICENSE_IMG_MISSING    = 7005; // 营业执照图片不存在或已过期
    public static final int FOOD_PERMIT_MISSING    = 7006; // 食品许可证图片不存在或已过期
    public static final int REVIEW_NOTE_TOO_LONG   = 7007; // 审核备注长度超限（10-200 字）
    public static final int DUPLICATE_APPLICATION  = 7008; // 该商家已有待审核申请，请勿重复提交

    // ==================== 品类管理域 8000 ~ 8999 ====================
    public static final int CATEGORY_NAME_EXISTS     = 8001; // 品类名称已存在
    public static final int CATEGORY_NAME_LENGTH     = 8002; // 品类名称长度 2-10 字
    public static final int CATEGORY_NOT_EXISTS      = 8003; // 品类不存在或已被删除
    public static final int CATEGORY_MUST_DISABLE    = 8004; // 请先停用该品类再删除
    public static final int CATEGORY_HAS_MERCHANTS   = 8005; // 品类下仍有商家关联，无法删除
    public static final int CATEGORY_SORT_INVALID    = 8006; // 排序权重必须为正整数
    public static final int CATEGORY_ICON_UPLOAD_FAIL = 8007; // 图标上传失败（OSS 异常）

}
