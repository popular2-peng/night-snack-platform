package com.nsp.common.constant;

/**
 * 业务异常消息常量 — 与 {@link ErrorCode} 错误码配对使用
 *
 * <p>规范：service/impl 中抛出的所有 {@code BizException} 消息必须引用本类常量，
 * 禁止在业务代码中直接写字符串字面量或"常量 + 字符串"拼接。
 * 含动态参数的消息用 {@code %s} 占位，调用处以 {@code String.format} 填充。
 *
 * <p>命名：常量名 = 语义前缀 + {@code _MSG} 后缀；同一错误码在不同场景使用不同文案时，
 * 通过前缀区分（如 {@link #FORBIDDEN_MERCHANT_BANNED_MSG} 与 {@link #ADDRESS_FORBIDDEN_MSG}）。
 */
public final class MessageConst {

    private MessageConst() { /* 工具类禁止实例化 */ }

    // ==================== 通用 1000 ~ 1999 ====================
    /** 商家已被封禁（FORBIDDEN） */
    public static final String FORBIDDEN_MERCHANT_BANNED_MSG = "商家已被封禁";
    /** 账号已被封禁（FORBIDDEN） */
    public static final String FORBIDDEN_ACCOUNT_BANNED_MSG = "账号已被封禁";
    /** 申请缺少商家登录用户名（PARAM_ERROR） */
    public static final String PARAM_APPLICATION_MISSING_USERNAME_MSG = "申请缺少商家登录用户名";
    /** 申请缺少商家登录密码（PARAM_ERROR） */
    public static final String PARAM_APPLICATION_MISSING_PASSWORD_MSG = "申请缺少商家登录密码";
    /** 不支持的支付渠道（PARAM_ERROR） */
    public static final String PARAM_PAY_CHANNEL_UNSUPPORTED_MSG = "不支持的支付渠道: %s";
    /** 不支持的业务类型（PARAM_ERROR） */
    public static final String PARAM_UPLOAD_BIZ_TYPE_INVALID_MSG = "不支持的业务类型: %s";
    /** 库存扣减脚本执行异常（SYSTEM_ERROR） */
    public static final String SYSTEM_STOCK_SCRIPT_ERROR_MSG = "库存扣减脚本执行异常";
    /** 上传签名生成失败（SYSTEM_ERROR） */
    public static final String SYSTEM_UPLOAD_SIGN_FAIL_MSG = "上传签名生成失败";
    /** 获取文件访问链接失败（SYSTEM_ERROR） */
    public static final String SYSTEM_UPLOAD_URL_FAIL_MSG = "获取文件访问链接失败";

    // ==================== 用户域 2000 ~ 2999 ====================
    /** 用户不存在（USER_NOT_FOUND） */
    public static final String USER_NOT_FOUND_MSG = "用户不存在";
    /** 管理员不存在（USER_NOT_FOUND） */
    public static final String ADMIN_NOT_FOUND_MSG = "管理员不存在";
    /** 用户登录请使用手机号（PARAM_ERROR） */
    public static final String USER_LOGIN_REQUIRE_PHONE_MSG = "用户登录请使用手机号";
    /** 手机号未注册（USER_NOT_FOUND） */
    public static final String USER_PHONE_NOT_REGISTERED_MSG = "手机号未注册";
    /** 该手机号未注册商家（USER_NOT_FOUND） */
    public static final String USER_PHONE_NOT_MERCHANT_MSG = "该手机号未注册商家";
    /** 密码错误（PASSWORD_ERROR） */
    public static final String PASSWORD_ERROR_MSG = "密码错误";
    /** 用户名已存在（USERNAME_EXISTS） */
    public static final String USERNAME_EXISTS_MSG = "用户名已存在";
    /** 账号已被占用，请更换（USERNAME_EXISTS） */
    public static final String USERNAME_TAKEN_MSG = "账号已被占用，请更换";
    /** 手机号已注册（PHONE_EXISTS） */
    public static final String PHONE_EXISTS_MSG = "手机号已注册";
    /** 手机号已注册，请直接登录（PHONE_EXISTS） */
    public static final String PHONE_EXISTS_LOGIN_MSG = "手机号已注册，请直接登录";
    /** 地址不存在（ADDRESS_NOT_FOUND） */
    public static final String ADDRESS_NOT_FOUND_MSG = "地址不存在";
    /** 地址数量已达上限（ADDRESS_LIMIT_EXCEEDED），参数：上限值 */
    public static final String ADDRESS_LIMIT_EXCEEDED_MSG = "地址数量已达上限（%s）";
    /** 无权操作他人地址（FORBIDDEN） */
    public static final String ADDRESS_FORBIDDEN_MSG = "无权操作他人地址";
    /** 单个商品数量超过上限（CART_ITEM_LIMIT），参数：上限值 */
    public static final String CART_ITEM_LIMIT_MSG = "单个商品数量不能超过 %s";
    /** 购物车商品不属于该商家（PRODUCT_NOT_FOUND） */
    public static final String CART_PRODUCT_NOT_BELONG_MSG = "商品不属于该商家";
    /** 用户已被封禁，无需重复操作（USER_ALREADY_BANNED） */
    public static final String USER_ALREADY_BANNED_MSG = "用户已被封禁，无需重复操作";
    public static final String USER_ACTIVE_CANNOT_DELETE_MSG = "启用账号不可删除，请先封禁";
    /** 用户未被封禁，无法解封（USER_NOT_BANNED） */
    public static final String USER_NOT_BANNED_MSG = "用户未被封禁，无法解封";
    /** 封禁时长不能为负数（USER_BAN_DURATION_INVALID） */
    public static final String USER_BAN_DURATION_INVALID_MSG = "封禁时长不能为负数";
    /** 用户已被封禁，无法重置密码（USER_RESET_PASSWORD_FAIL） */
    public static final String USER_RESET_PASSWORD_BANNED_MSG = "用户已被封禁，无法重置密码";

    // ==================== 商家/商品域 3000 ~ 3999 ====================
    /** 商家不存在（MERCHANT_NOT_FOUND） */
    public static final String MERCHANT_NOT_FOUND_MSG = "商家不存在";
    /** 商家已打烊或未审核通过（MERCHANT_CLOSED） */
    public static final String MERCHANT_CLOSED_MSG = "商家已打烊或未审核通过";
    /** 商家尚未通过审核（MERCHANT_NOT_APPROVED） */
    public static final String MERCHANT_NOT_APPROVED_MSG = "商家尚未通过审核";
    /** 商家已被封禁，无需重复操作（MERCHANT_ALREADY_BANNED） */
    public static final String MERCHANT_ALREADY_BANNED_MSG = "商家已被封禁，无需重复操作";
    /** 商家未被封禁，无法解封（MERCHANT_NOT_BANNED） */
    public static final String MERCHANT_NOT_BANNED_MSG = "商家未被封禁，无法解封";
    /** 商家已处于打烊状态（ORDER_STATUS_ERROR） */
    public static final String MERCHANT_CLOSED_STATUS_MSG = "商家已处于打烊状态";
    /** 商家已在营业中（ORDER_STATUS_ERROR） */
    public static final String MERCHANT_OPEN_STATUS_MSG = "商家已在营业中";
    /** 非本店商家，无权操作（NOT_MERCHANT_OWNER） */
    public static final String NOT_MERCHANT_OWNER_MSG = "非本店商家，无权操作";
    /** 非本店商家（NOT_MERCHANT_OWNER，简短文案） */
    public static final String NOT_MERCHANT_OWNER_SHORT_MSG = "非本店商家";
    /** 无权查看该店铺订单（NOT_MERCHANT_OWNER） */
    public static final String NOT_MERCHANT_ORDER_VIEW_MSG = "无权查看该店铺订单";
    /** 无权操作该商品（NOT_MERCHANT_OWNER） */
    public static final String PRODUCT_NOT_OWNER_MSG = "无权操作该商品";
    /** 商品不存在（PRODUCT_NOT_FOUND） */
    public static final String PRODUCT_NOT_FOUND_MSG = "商品不存在";
    /** 商品已下架（PRODUCT_OFF_SHELF），参数：商品名 */
    public static final String PRODUCT_OFF_SHELF_MSG = "商品已下架: %s";
    /** 商品不属于该商家（PRODUCT_NOT_FOUND），参数：specId */
    public static final String PRODUCT_NOT_BELONG_MERCHANT_MSG = "商品不属于该商家: specId=%s";
    /** 规格不存在（SPEC_NOT_FOUND） */
    public static final String SPEC_NOT_FOUND_MSG = "规格不存在";
    /** 规格不存在（SPEC_NOT_FOUND），参数：specId */
    public static final String SPEC_NOT_FOUND_WITH_ID_MSG = "规格不存在: %s";
    /** 品类不存在（CATEGORY_NOT_FOUND） */
    public static final String CATEGORY_NOT_FOUND_MSG = "品类不存在";

    // ==================== 订单域 4000 ~ 4999 ====================
    /** 订单不存在（ORDER_NOT_FOUND） */
    public static final String ORDER_NOT_FOUND_MSG = "订单不存在";
    /** 无权操作该订单（FORBIDDEN） */
    public static final String ORDER_FORBIDDEN_MSG = "无权操作该订单";
    /** 无权查看该订单（FORBIDDEN） */
    public static final String ORDER_VIEW_FORBIDDEN_MSG = "无权查看该订单";
    /** 收货地址不存在（ADDRESS_NOT_FOUND） */
    public static final String ORDER_ADDRESS_NOT_FOUND_MSG = "收货地址不存在";
    /** 超出配送范围（OUT_OF_DELIVERY_RANGE），参数：距离、配送上限 */
    public static final String ORDER_OUT_OF_DELIVERY_RANGE_MSG = "超出配送范围，距离 %s 米（限 %s 米）";
    /** 商家非营业时间（NOT_IN_BUSINESS_HOURS），参数：开始、结束 */
    public static final String ORDER_NOT_IN_BUSINESS_HOURS_MSG = "商家非营业时间（%s ~ %s）";
    /** 当前状态不允许支付（ORDER_STATUS_ERROR），参数：状态描述 */
    public static final String ORDER_STATUS_PAY_MSG = "当前状态不允许支付: %s";
    /** 只有已支付但未接单的订单可以拒单（ORDER_STATUS_ERROR） */
    public static final String ORDER_STATUS_REJECT_MSG = "只有已支付但未接单的订单可以拒单";
    /** 只有待支付订单可以取消（ORDER_STATUS_ERROR） */
    public static final String ORDER_STATUS_CANCEL_MSG = "只有待支付订单可以取消";
    /** 只有已支付订单可以接单（ORDER_STATUS_ERROR） */
    public static final String ORDER_STATUS_ACCEPT_MSG = "只有已支付订单可以接单";
    /** 只有已接单订单可以开始制作（ORDER_STATUS_ERROR） */
    public static final String ORDER_STATUS_COOKING_MSG = "只有已接单订单可以开始制作";
    /** 只有制作中订单可以标记完成（ORDER_STATUS_ERROR） */
    public static final String ORDER_STATUS_COMPLETE_MSG = "只有制作中订单可以标记完成";
    /** 库存不足（STOCK_INSUFFICIENT），参数：specId */
    public static final String STOCK_INSUFFICIENT_MSG = "库存不足: specId=%s";
    /** 库存未初始化（SPEC_NOT_FOUND），参数：specId */
    public static final String STOCK_NOT_INITIALIZED_MSG = "库存未初始化: specId=%s";

    // ==================== 支付域 5000 ~ 5999 ====================
    /** 未找到支付记录（PAY_ORDER_NOT_FOUND） */
    public static final String PAY_RECORD_NOT_FOUND_MSG = "未找到支付记录";
    /** 支付宝回调验签失败（PAY_SIGN_ERROR） */
    public static final String PAY_ALIPAY_SIGN_ERROR_MSG = "支付宝回调验签失败";

    // ==================== 上传域 6000 ~ 6999 ====================
    /** 签名请求过于频繁（UPLOAD_SIGN_LIMIT），参数：每分钟上限 */
    public static final String UPLOAD_SIGN_LIMIT_MSG = "签名请求过于频繁，每分钟限 %s 次";
    /** 不支持的文件类型（UPLOAD_TYPE_INVALID），参数：扩展名、允许列表 */
    public static final String UPLOAD_TYPE_INVALID_MSG = "不支持的文件类型: %s，允许: %s";

    // ==================== 商家管理域 7000 ~ 7999 ====================
    /** 申请不存在（MERCHANT_NOT_FOUND） */
    public static final String APPLICATION_NOT_FOUND_MSG = "申请不存在";
    /** 申请人不存在（USER_NOT_FOUND） */
    public static final String APPLICANT_NOT_FOUND_MSG = "申请人不存在";

    // ==================== 品类管理域 8000 ~ 8999 ====================
    /** 品类名称不能为空（CATEGORY_NAME_LENGTH） */
    public static final String CATEGORY_NAME_EMPTY_MSG = "品类名称不能为空";
    /** 品类名称长度 2-10 字（CATEGORY_NAME_LENGTH） */
    public static final String CATEGORY_NAME_LENGTH_MSG = "品类名称长度 2-10 字";
    /** 品类名称已存在（CATEGORY_NAME_EXISTS） */
    public static final String CATEGORY_NAME_EXISTS_MSG = "品类名称已存在";
    /** 排序权重必须为非负整数（CATEGORY_SORT_INVALID） */
    public static final String CATEGORY_SORT_INVALID_MSG = "排序权重必须为非负整数";
    /** 主营品类不存在（CATEGORY_NOT_EXISTS） */
    public static final String CATEGORY_NOT_EXISTS_MSG = "主营品类不存在";
    /** 主营品类已停用（CATEGORY_NOT_EXISTS） */
    public static final String CATEGORY_DISABLED_MSG = "主营品类已停用";
    /** 品类不存在或已被删除（CATEGORY_NOT_EXISTS） */
    public static final String CATEGORY_NOT_EXISTS_OR_DELETED_MSG = "品类不存在或已被删除";
    /** 品类不存在（CATEGORY_NOT_EXISTS），参数：id */
    public static final String CATEGORY_NOT_EXISTS_WITH_ID_MSG = "品类不存在: id=%s";
    /** 请先停用该品类再删除（CATEGORY_MUST_DISABLE） */
    public static final String CATEGORY_MUST_DISABLE_MSG = "请先停用该品类再删除";
    /** 品类下仍有商家关联，无法删除（CATEGORY_HAS_MERCHANTS） */
    public static final String CATEGORY_HAS_MERCHANTS_MSG = "该品类下仍有商家关联，无法删除";
    /** 品类下仍有商家关联，无法停用（CATEGORY_HAS_MERCHANTS） */
    public static final String CATEGORY_HAS_MERCHANTS_DISABLE_MSG = "该品类下仍有商家关联，无法停用";

}
