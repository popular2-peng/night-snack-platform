# 夜宵外卖平台 API 文档

> 后端地址：`http://localhost:8080`  
> Swagger UI：`http://localhost:8080/swagger-ui.html`（匿名可访问，可导入 Apifox）

---

## 一、统一规范

### 1.1 响应格式

所有接口返回统一结构 `Result<T>`：

```json
{
  "code": 0,
  "msg": "ok",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务状态码，`0` 表示成功 |
| `msg` | string | 提示信息 |
| `data` | T | 业务数据，分页时为 `{ "total": N, "list": [...] }` |

### 1.2 鉴权

除登录/注册/支付回调/OSS 回调外，所有接口需在 Header 中携带 Token：

```
Authorization: Bearer <jwt>
```

Token 有效期 2 小时，Refresh Token 有效期 7 天。

**Token 刷新契约：**

- Access Token 用于所有业务接口；Refresh Token **仅用于** `POST /api/auth/refresh`（见 3.8），业务接口收到 Refresh Token 直接拒绝（HTTP 401 + `1004`）
- Access Token 过期时，前端调用刷新接口换取新双令牌并重试原请求（无感续期），无需重新登录
- 双令牌轮换：每次刷新签发新的 Access + Refresh；旧 Refresh 到期前仍可用（MVP 取舍，正式环境可加 jti 拉黑）
- 被封禁/审核中的账号刷新时被拒（`1002` / `3003`）—— 封禁后最长 2 小时（Access 到期）完全失效
- 401 响应体统一为 `Result` JSON：`1001` 未登录 / `1003` Token 过期 / `1004` Token 无效

### 1.3 分页

统一参数 `?page=1&size=20`，响应格式：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "total": 100,
    "list": [...]
  }
}
```

### 1.4 时间格式

所有时间字段统一使用 ISO-8601 格式：`"2025-07-18T21:30:00"`（后端全局 Jackson 配置强制，管理端 VO 已同步整改）

### 1.5 批量接口契约

所有 `POST /api/admin/**/batch-*` 接口遵循统一契约：

- 请求体：`{ "ids": [1, 2, 3] }`，**单次最多 100 条**（超出返回 `1100`）
- 响应体：`BatchResultVO`，**部分成功不算失败**（HTTP 200）

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "successCount": 7,
    "skippedCount": 3,
    "skippedReasons": ["2 个账号已封禁", "1 个账号不存在"],
    "message": "成功处理 7 项，跳过 3 项：2 个账号已封禁、1 个账号不存在"
  }
}
```

- 语义：不满足操作条件的条目**跳过**（计入 `skippedCount` 并在 `skippedReasons`/`message` 汇总原因），不满足条件的条目**不算失败**
- 各接口的跳过规则见对应小节（如批量退款：未支付/已完成/已退款/不存在均跳过）

---

## 二、错误码

### 2.1 成功码

| code | 说明 |
|------|------|
| `0` | 请求成功 |

### 2.2 通用错误（1000 ~ 1999）

| code | 说明 |
|------|------|
| `1001` | 未登录（HTTP 401 + 响应体返回） |
| `1002` | 无权限 |
| `1003` | Token 已过期（HTTP 401 + 响应体返回） |
| `1004` | Token 无效（HTTP 401 + 响应体返回） |
| `1100` | 参数校验失败（HTTP 400 + 响应体返回） |
| `1999` | 系统内部错误 |

### 2.3 用户域（2000 ~ 2999）

| code | 说明 |
|------|------|
| `2001` | 用户名已存在 |
| `2002` | 手机号已注册 |
| `2003` | 用户不存在 |
| `2004` | 密码错误 |
| `2101` | 地址不存在 |
| `2102` | 地址数量超限 |
| `2201` | 购物车商品数量超限 |
| `2301` | 用户不存在或已被删除 |
| `2302` | 封禁原因不能为空 |
| `2303` | 用户已被封禁 |
| `2304` | 用户未被封禁 |
| `2305` | 重置密码失败（用户状态异常） |
| `2306` | 启用账号不可删除 |
| `2307` | 商家未封禁不可删除 |
| `2308` | 手机号格式不正确 |
| `2309` | 封禁时长不能为负数 |

### 2.4 商品/商家域（3000 ~ 3999）

| code | 说明 |
|------|------|
| `3001` | 商家不存在 |
| `3002` | 商家已打烊 |
| `3003` | 商家未通过审核 |
| `3101` | 商品不存在 |
| `3102` | 商品已下架 |
| `3103` | 规格不存在 |
| `3104` | 库存不足 |
| `3105` | 商品在售，请先停售再删除 |
| `3201` | 品类不存在 |
| `3301` | 非本店商家，无权操作 |

### 2.5 订单域（4000 ~ 4999）

| code | 说明 |
|------|------|
| `4001` | 订单不存在 |
| `4002` | 订单状态不允许此操作 |
| `4003` | 超出配送范围 |
| `4004` | 非营业时间 |
| `4005` | 购物车为空 |
| `4006` | 订单创建中，请勿重复提交 |

### 2.6 支付域（5000 ~ 5999）

| code | 说明 |
|------|------|
| `5001` | 支付订单不存在 |
| `5002` | 订单已支付（幂等） |
| `5003` | 支付签名校验失败 |
| `5004` | 支付渠道异常 |
| `5005` | 订单无成功支付记录，无法退款 |
| `5006` | 渠道无真实交易（内部降级用，不对外暴露） |
| `5007` | 退款调用失败，可重试 |

### 2.7 上传域（6000 ~ 6999）

| code | 说明 |
|------|------|
| `6001` | 上传签名请求过于频繁 |
| `6002` | 文件类型不允许 |
| `6003` | 文件大小超限 |
| `6004` | OSS 回调校验失败 |

### 2.8 商家入驻审核（7000 ~ 7999）

| code | 说明 |
|------|------|
| `7001` | 拒绝原因不能为空 |
| `7002` | 封禁原因不能为空 |
| `7003` | 商家已被封禁 |
| `7004` | 商家未被封禁 |
| `7005` | 营业执照图片缺失 |
| `7006` | 食品经营许可证图片缺失 |
| `7007` | 审核备注过长 |
| `7008` | 重复申请 |

### 2.9 品类管理（8000 ~ 8999）

| code | 说明 |
|------|------|
| `8001` | 品类名称已存在 |
| `8002` | 品类名称长度 2-10 字 |
| `8003` | 品类不存在或已被删除 |
| `8004` | 请先停用该品类再删除 |
| `8005` | 品类下仍有商家关联，无法删除 |
| `8006` | 排序权重必须为非负整数 |
| `8007` | 图标上传失败（OSS 异常） |

---

## 三、公开接口（无需 Token）

### 3.1 用户注册

```
POST /api/user/register
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 手机号（登录凭据） |
| `password` | string | 是 | 密码 |

> - 无需填写用户名：注册时系统自动生成「普通用户 + 8 位随机串」作为用户名（如 `普通用户06104202`）
> - 注册成功即自动登录，直接返回 Token（注册即登录），角色固定为 `USER`
> - 注册时间（`created_at`）由数据库列默认值 `CURRENT_TIMESTAMP`（服务器当前时间）写入，与接口返回的 `createdAt` 一致
> - 密码规则：6-20 位，必须同时包含字母和数字（可含 `!@#$%^&*()_+` 特殊字符），不满足返回 `1100`

**响应 data：**

```json
{
  "userId": 1,
  "username": "zhangsan",
  "role": "USER",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户 ID |
| `username` | string | 用户名 |
| `role` | string | `USER` |
| `accessToken` | string | 访问令牌（有效期 2 小时） |
| `refreshToken` | string | 刷新令牌（有效期 7 天） |

**可能错误码：** `2002`

---

### 3.2 用户登录（用户专用）

```
POST /api/user/login
```

> **用户专用登录接口**：仅支持手机号 + 密码，不兼容用户名、商家、管理员（商家走 3.3，管理员走 3.4）。

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 用户手机号（注册时填写） |
| `password` | string | 是 | 密码 |

**匹配规则：** 仅查 `user` 表的 `phone` 字段，命中且校验通过 → `USER` Token；未命中 → `2003`「手机号未注册」；未传 `phone` → `1100`「用户登录请使用手机号」

> 登录成功后，后端实时更新该用户 `last_login_at`（最后登录时间），管理端用户列表据此展示。

**响应 data：**

```json
{
  "userId": 1,
  "username": "tester",
  "role": "USER",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户 ID |
| `username` | string | 用户名 |
| `role` | string | `USER` |
| `accessToken` | string | 访问令牌（有效期 2 小时） |
| `refreshToken` | string | 刷新令牌（有效期 7 天） |

**可能错误码：** `1100`（未传手机号）, `2003`, `2004`, `1002`（账号被封禁）

---

### 3.3 商家登录

```
POST /api/merchant/login
```

> 商家专用登录接口，仅查商家表。

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 商家注册手机号 |
| `password` | string | 是 | 密码 |

**拦截规则：**
- `SUSPENDED`（已封禁）→ `1002`「商家已被封禁」
- `PENDING`（审核中）→ `3003`「商家尚未通过审核」

> 登录成功后，后端实时更新该商家 `last_login_at`（最后登录时间），管理端商家列表据此展示。

**响应 data：** 同 3.2，`role` 固定为 `MERCHANT`

**可能错误码：** `2003`（该手机号未注册商家）, `2004`, `1002`, `3003`

---

### 3.4 管理员登录（管理员专用）

```
POST /api/admin/login
```

> 管理员专用登录接口，仅查 `admin` 表。

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 管理员用户名 |
| `password` | string | 是 | 密码 |

**响应 data：** 同 3.2，`role` 固定为 `ADMIN`

**可能错误码：** `2003`（管理员不存在）, `2004`, `1002`（管理员被封禁）

---

### 3.5 商家入驻申请（注册）

```
POST /api/merchant/register
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 登录账号（审核通过后即为商家登录账号；前端自动生成，如 `m` + 10 位随机） |
| `password` | string | 是 | 登录密码（BCrypt 加密存储） |
| `phone` | string | 是 | 联系人手机号（商家登录凭据） |
| `ownerName` | string | 是 | 联系人姓名（申请人/店主，2-20 个中文字符，允许少数民族姓名间隔号 `·`，如"阿不都·热合曼"） |
| `shopName` | string | 是 | 店铺名称 |
| `categoryId` | long | 是 | 主营品类 ID（`category` 表，须为 `ENABLED`） |
| `province` | string | 是 | 所在省（如"广东省"） |
| `city` | string | 是 | 所在市（如"深圳市"） |
| `district` | string | 是 | 所在区县（如"南山区"） |
| `address` | string | 是 | 详细地址（提交后后端调用百度地图地理编码转换为坐标） |
| `logoUrl` | string | 是 | 店铺 LOGO OSS URL（必传，JPG/PNG，OSS 直传成功后回传 URL；缺失返回 `1100`） |
| `description` | string | 否 | 店铺介绍 |

> - 密码规则同 3.1：6-20 位，必须同时包含字母和数字
> - 提交后创建 `PENDING` 入驻申请 + 申请人账号；管理员审核通过后**直接复用本次填写的账号密码**创建商家，不生成默认密码。
> - 地址坐标：后端调用百度地理编码接口（`geocoding/v3`）将「省+市+区县+详细地址」转换为 `lat/lng` 存入申请；AK 未配置或校验失败时坐标为空，申请仍可提交（审核通过创建商家时坐标待补充）。

**可能错误码：** `2001`（账号已被占用）, `2002`（手机号已注册）, `8003`（主营品类不存在/已停用）

---

### 3.6 平台启用品类（商家入驻申请页下拉，公开）

```
GET /api/merchant/category/all
```

> 无需 Token。返回 `List<CategoryVO>`（仅 `status=ENABLED` 的品类，供注册/入驻表单下拉使用）。

---

### 3.7 支付宝支付回调（匿名）

```
POST /api/pay/alipay/callback
```

> 支付宝异步通知回调，由支付宝服务器发起，返回 `"success"` 或 `"fail"` 字符串（非 JSON）。

**可能错误码：** `5001`, `5003`

---

### 3.8 Token 刷新（三端通用，匿名）

```
POST /api/auth/refresh
```

> 无需 Authorization Header：Refresh Token 本身即凭证（白名单接口，服务端校验令牌类型与账号状态）。
> 用于 Access Token（2 小时）过期后的无感续期 —— 前端在收到 401/`1003` 时自动调用并重试原请求。

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refreshToken` | string | 是 | 刷新令牌（登录/注册时签发，有效期 7 天） |

**响应 data：** `RefreshTokenVO`（双令牌轮换）

```json
{
  "userId": 1,
  "role": "ADMIN",
  "accessToken": "eyJ...（新签发，2 小时）",
  "refreshToken": "eyJ...（新签发，7 天）"
}
```

**校验规则：**

- Access Token 或旧版无类型 Token 用于刷新 → `1004`
- 账号状态：用户/管理员被封禁 → `1002`；商家被封禁 → `1002`，商家审核中 → `3003`（封禁后最长 2 小时即完全失效）
- 每次刷新签发**新的** Access + Refresh（轮换）；旧 Refresh 到期前仍可用（MVP 取舍）

**可能错误码：** `1003`（Refresh 过期）, `1004`, `1002`, `3003`

---

## 四、用户端接口（`/api/user/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`USER`（部分接口支持多角色）

### 4.1 用户信息

#### 获取当前用户信息

```
GET /api/user/info
```

> 权限：`USER` / `MERCHANT` / `ADMIN`（登录即可）

**响应 data：**

```json
{
  "id": 1,
  "username": "zhangsan",
  "phone": "13800138000",
  "role": "USER",
  "status": "ACTIVE",
  "avatarUrl": "https://nsp.oss-cn-shenzhen.aliyuncs.com/avatar/1/xxx.jpg",
  "createdAt": "2025-07-01T00:00:00"
}
```

**可能错误码：** `2003`

---

### 4.2 收货地址

#### 获取地址列表

```
GET /api/user/address
```

**响应 data：** `AddressVO[]`

---

#### 获取单个地址

```
GET /api/user/address/{id}
```

**可能错误码：** `2101`

---

#### 新增地址

```
POST /api/user/address
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `receiver` | string | 是 | 收货人 |
| `phone` | string | 是 | 收货电话（`^1[3-9]\d{9}$`，格式错误返回 `1100`） |
| `detail` | string | 是 | 详细地址 |
| `lat` | number | 否 | 纬度（移动端暂不支持地图选点；为空时下单距离校验按 0 处理，MVP 降级） |
| `lng` | number | 否 | 经度（同 lat） |
| `isDefault` | boolean | 否 | 是否设为默认地址（设默认时自动取消原默认，互斥） |

**可能错误码：** `2102`

---

#### 更新地址

```
PUT /api/user/address/{id}
```

**请求体：** 同新增

**可能错误码：** `2101`

---

#### 删除地址

```
DELETE /api/user/address/{id}
```

**可能错误码：** `2101`

---

#### 设为默认地址

```
PUT /api/user/address/{id}/default
```

**可能错误码：** `2101`

---

### 4.3 购物车

#### 查询购物车

```
GET /api/user/cart
```

**响应 data：** `CartVO`（含商品列表 + 总金额）

---

#### 添加商品到购物车

```
POST /api/user/cart?merchantId={merchantId}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | long | 是 | 商家 ID，跨店会提示清空其他店铺商品 |

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `specId` | long | 是 | 规格 ID |
| `quantity` | int | 是 | 数量 |

**可能错误码：** `2201`

---

#### 更新商品数量

```
PUT /api/user/cart
```

**请求体：** 同添加

---

#### 移除购物车商品

```
DELETE /api/user/cart/{specId}
```

---

#### 清空购物车

```
DELETE /api/user/cart
```

---

### 4.4 订单

> **订单号规则**：`NSP + yyyyMMdd + 当天 6 位自增序号`（如 `NSP20260811000001`）。
> 序号由 Redis 原子自增（key `order:no:{yyyyMMdd}`，按天分桶，TTL 2 天）生成 —— 下单时即预占序号，
> 多名用户并发下单也不会产生重复订单号，同一天上限 999999 单；Redis 不可用时降级为「时间戳 + 4 位随机数」。
> 订单号中的日期始终等于订单创建时间（`created_at`）。订单 `created_at` 由服务器当前时间写入（列默认 `CURRENT_TIMESTAMP`），与状态流转日志首条（下单）时间一致；列表接口默认按订单 ID 降序返回（最新订单在前）。

> **订单超时自动任务**（后端每 60 秒扫描一次，操作人记录为 `system`）：
>
> | 规则 | 触发条件 | 动作 |
> |------|---------|------|
> | 超时未支付 | 下单后 15 分钟仍为 `PENDING_PAY` | 自动取消 `CANCELLED`，释放预扣库存 |
> | 超时未接单 | 支付后 15 分钟商家未接单（`PAID`） | 自动取消 `CANCELLED`，释放库存 + **自动退款**（支付宝沙箱真实退款 / 微信 mock；失败保持可重试） |
> | 超时自动完成 | 制作中（`COOKING`）超过 30 分钟 | 自动完成 `COMPLETED`（视为已送达） |
>
> 状态停留时长依据该状态下最后一条 `order_status_log.created_at` 判定（不依赖 `updated_at`）。

#### 创建订单（下单）

```
POST /api/user/order
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | long | 是 | 商家 ID |
| `addressId` | long | 是 | 收货地址 ID |
| `items` | array | 是 | 订单明细：`[{ "specId": 1, "quantity": 2 }]` |
| `payChannel` | string | 是 | 支付渠道：`ALIPAY` / `WECHAT_MOCK` |
| `clientToken` | string | 是 | 幂等键（客户端生成的唯一串，如 UUID，最长 64 位） |
| `userLat` | number | 否 | 用户当前纬度（参考值，服务端以地址坐标为准） |
| `userLng` | number | 否 | 用户当前经度 |

> **下单幂等**：服务端用 Redis `setIfAbsent` 拦截重复提交（key `order:client-token:{userId}:{clientToken}`，TTL 10 分钟）：
> - 同一 `clientToken` 重复下单 → **返回第一次已创建的订单**（同一订单号，幂等语义）
> - 首个请求仍在处理中时重复提交 → `4006`「订单创建中，请勿重复提交」
> - 下单在**库存预扣（Lua）之前**完成幂等拦截，并发重复请求不会二次扣库存

**可能错误码：** `3001`, `3002`, `3101`, `3102`, `3103`, `3104`, `4003`, `4004`, `4006`

---

#### 我的订单列表

```
GET /api/user/order?page=1&size=20
```

**响应 data：** `PageVO<OrderListVO>`

---

#### 订单详情

```
GET /api/user/order/{id}
```

**可能错误码：** `4001`

---

#### 取消订单

```
POST /api/user/order/{id}/cancel
```

> 仅 `PENDING_PAY` 状态可取消，取消后释放预扣库存。

**可能错误码：** `4001`, `4002`

---

### 4.5 支付

#### 发起支付

```
POST /api/user/pay
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderNo` | string | 是 | 订单编号 |
| `channel` | string | 是 | 支付渠道：`ALIPAY` / `WECHAT_MOCK` |

**响应 data：**

```json
{
  "orderNo": "NSP20250723...",
  "channel": "ALIPAY",
  "qrCode": "https://...",
  "status": "PENDING"
}
```

**可能错误码：** `5002`, `5004`

---

#### 查询支付状态

```
GET /api/user/pay/{orderNo}/status
```

**可能错误码：** `5001`

---

### 4.6 公共浏览

#### 商家列表

```
GET /api/merchant/list?lat=39.99632&lng=116.48090&page=1&size=20
```

> 按距离排序（近→远），百度地图 API + Haversine 降级；响应为分页结构 `PageVO`（`{ total, list }`，list 内每项含 `distance` 米）。
> `page`/`size` 默认 1/20，不传亦可（兼容旧调用）。

**响应 data：** `PageVO<MerchantVO>`

---

#### 商家详情

```
GET /api/merchant/{id}
```

**可能错误码：** `3001`

---

#### 商品列表

```
GET /api/merchant/product/list?merchantId={merchantId}&categoryId={categoryId}&page=1&size=20&keyword={name}&status={status}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | long | 是 | 商家 ID（用户端浏览指定商家；商家管理页传自己的 ID） |
| `categoryId` | long | 否 | 所属品类筛选 |
| `page` / `size` | int | 否 | 分页，默认 1/20 |
| `keyword` | string | 否 | 商品名称模糊搜索 |
| `status` | string | 否 | 状态筛选：`ON`(在售) / `OFF`(停售)，非法值忽略 |

**响应 data：** `PageVO<ProductVO>`（`{ total, list }`，list 每项含 `specs` 规格列表）

---

#### 商品详情

```
GET /api/merchant/product/{id}
```

**可能错误码：** `3101`

---

## 五、商家端接口（`/api/merchant/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`MERCHANT`
>
> **商家身份由 JWT 解析**：商家登录时 JWT 的 `sub` 即 `merchant.id`，本节所有接口**不再接收 `merchantId` 参数**（从源头杜绝越权传参）。归属校验仍在服务层（`3301` 非本店商家）作为纵深防御保留。
> 例外：`GET /api/merchant/product/list`、`GET /api/merchant/list`、`GET /api/merchant/{id}` 为多角色浏览接口，保留 `merchantId` 查询参数（用户端浏览商家商品时需显式指定商家）。

### 5.1 店铺管理

#### 获取店铺信息（当前登录商家）

```
GET /api/merchant/shop
```

**响应 data：** `MerchantVO`（当前登录商家的完整店铺信息，含 `logoUrl` / `description` / `notice` / `businessHoursStart|End`（HH:mm）/ `deliveryRadius` / `address` / `lat` / `lng`）

---

#### 更新店铺信息（部分更新）

```
PUT /api/merchant/shop
```

> 全字段可选，**仅更新非空字段**；商家身份由 JWT 解析，无需传 merchantId。

**请求体：**

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| `name` | string | 否 | 2-20 字 | 店铺名称 |
| `logoUrl` | string | 否 | ≤500 | 店铺 LOGO OSS URL（先走 /api/upload/sign 直传后回传） |
| `address` | string | 否 | ≤200 | 详细地址（变更后服务端调百度地理编码重新解析坐标；解析失败保留原坐标并告警） |
| `businessHoursStart` | string | 否 | `HH:mm` | 营业开始时间，**必须与结束时间成对填写**（单独传返回 `1100`） |
| `businessHoursEnd` | string | 否 | `HH:mm` | 营业结束时间（支持跨天，如 `02:00`） |
| `deliveryRadius` | int | 否 | 500-10000 | 配送半径（米），默认 5000 |
| `description` | string | 否 | ≤200 | 店铺简介 |
| `notice` | string | 否 | ≤200 | 店铺公告 |

**响应 data：** `MerchantVO`（更新后的完整店铺信息）

**可能错误码：** `3001`, `1100`

---

#### 更新营业状态

```
PUT /api/merchant/shop/status?status={status}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `OPEN`(营业中) / `CLOSED`(打烊) |

**可能错误码：** `3001`, `3301`

---

### 5.2 品类管理

#### 品类列表（当前登录商家）

```
GET /api/merchant/category
```

---

#### 新增品类

```
POST /api/merchant/category?name={name}&sort=0
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | string | 是 | - | 品类名称 |
| `sort` | int | 否 | 0 | 排序 |

---

#### 更新品类（仅限本店品类）

```
PUT /api/merchant/category/{id}?name={name}&sort={sort}
```

**可能错误码：** `3201`

---

#### 删除品类（仅限本店品类）

```
DELETE /api/merchant/category/{id}
```

**可能错误码：** `3201`

---

### 5.3 商品管理

#### 新增商品（归属当前登录商家）

```
POST /api/merchant/product
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 商品名称 |
| `price` | number | 是 | 基础价格 |
| `imageUrl` | string | 否 | 商品图片 OSS URL |
| `description` | string | 否 | 商品描述 |
| `categoryId` | long | 是 | 所属品类 ID |
| `specs` | array | 否 | 规格列表 |

---

#### 更新商品（仅限本店商品）

```
PUT /api/merchant/product/{id}
```

**请求体：** 同新增

**可能错误码：** `3101`

---

#### 上架/下架（仅限本店商品）

```
PUT /api/merchant/product/{id}/status?status={status}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `ON`(上架) / `OFF`(下架) |

**可能错误码：** `3101`

---

#### 删除商品（仅停售状态可删）

```
DELETE /api/merchant/product/{id}
```

> 软删除商品并**级联删除其规格**。前置条件：商品状态必须为 `OFF`（停售），在售商品删除返回 `3105`。

**可能错误码：** `3101`, `3301`, `3105`

---

#### 添加规格（仅限本店商品）

```
POST /api/merchant/product/{productId}/spec
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 规格名称（如"大杯/加珍珠"） |
| `priceDelta` | number | 是 | 价格差额（正数加价，0 基础价） |
| `stock` | int | 是 | 库存数量 |

**可能错误码：** `3101`

---

#### 删除规格（仅限本店商品）

```
DELETE /api/merchant/product/spec/{specId}
```

**可能错误码：** `3103`

---

### 5.4 订单管理

#### 店铺订单列表（当前登录商家）

```
GET /api/merchant/order?page=1&size=20&status={status}&dateStart={date}&dateEnd={date}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 1 |
| `size` | int | 否 | 每页条数，默认 20 |
| `status` | string | 否 | 状态筛选：`PENDING_PAY`/`PAID`/`ACCEPTED`/`COOKING`/`COMPLETED`/`CANCELLED`/`REFUNDING`/`REFUNDED`（非法值忽略，不传返回全部） |
| `dateStart` | string | 否 | 下单起始日期（`yyyy-MM-dd`，含当天）。**今日订单 = dateStart=dateEnd=今天** |
| `dateEnd` | string | 否 | 下单截止日期（`yyyy-MM-dd`，含当天） |

**响应 data：** `PageVO<OrderListVO>`（含 `items` 商品明细与 `addressSnapshot` 地址快照，订单卡片可直接渲染）

---

#### 订单状态分布统计

```
GET /api/merchant/order/stats?dateStart={date}&dateEnd={date}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `dateStart` | string | 否 | 统计起始日期（`yyyy-MM-dd`，含当天）。**今日订单角标 = dateStart=dateEnd=今天** |
| `dateEnd` | string | 否 | 统计截止日期（`yyyy-MM-dd`，含当天）。不传则统计全部历史订单 |

**响应 data：** `Map<String, Long>`（状态码 → 数量，如 `{"PAID": 2, "COOKING": 1, "COMPLETED": 8}`，用于订单页 tab 角标；全部订单页不展示角标）

---

#### 订单详情

```
GET /api/merchant/order/{id}
```

**可能错误码：** `4001`

---

#### 接单

```
PUT /api/merchant/order/{id}/accept
```

> 状态迁移：`PAID` → `ACCEPTED`

**可能错误码：** `4001`, `4002`

---

#### 开始制作

```
PUT /api/merchant/order/{id}/cooking
```

> 状态迁移：`ACCEPTED` → `COOKING`

**可能错误码：** `4001`, `4002`

---

#### 完成制作

```
PUT /api/merchant/order/{id}/complete
```

> 状态迁移：`COOKING` → `COMPLETED`

**可能错误码：** `4001`, `4002`

---

#### 拒单

```
PUT /api/merchant/order/{id}/reject?reason={reason}
```

> 状态迁移：`PAID` → `CANCELLED`，**自动退款**（支付宝沙箱真实退款 / 微信 mock；退款失败不阻断拒单，可由管理员批量退款重试）

**可能错误码：** `4001`, `4002`

---

### 5.5 WebSocket 实时推送（新订单通知）

```
ws://localhost:8080/ws/merchant?token=<accessToken>
```

> 商家端长连接：用户支付成功后，服务端实时推送新订单消息，商家端无需轮询。
> 前端开发环境经 Vite 代理 `/ws` 转发到 8080（`ws: true`）。

**握手鉴权：** query 参数 `token` 必须是**有效的商家 ACCESS 令牌**（`token_type=ACCESS` + `role=MERCHANT`，JWT 的 sub 即 merchantId）；无效/过期/刷新令牌/非商家角色 → 握手拒绝（非 101）。

**服务端 → 客户端消息（JSON 文本帧）：**

| type | 触发时机 | data 字段 |
|------|---------|-----------|
| `CONNECTED` | 握手成功建立连接 | 无 |
| `PONG` | 心跳响应 | 无 |
| `NEW_ORDER` | 订单支付成功（WECHAT_MOCK 直付 / 支付宝回调，统一入口 `markOrderPaid`） | `orderId` / `orderNo` / `totalAmount` / `createdAt`（ISO-8601） |

```json
{ "type": "NEW_ORDER", "data": { "orderId": 15, "orderNo": "NSP20260814000012", "totalAmount": 22.00, "createdAt": "2026-08-14T02:30:00" } }
```

**客户端 → 服务端：** `{"type":"PING"}` 心跳（建议 30 秒一次）。

**语义：**
- 一个商家可多端同时在线，新订单推送到该商家的**全部会话**
- 推送尽力而为：商家无在线会话时静默跳过，发送失败不影响支付主流程
- 商家端收到 `NEW_ORDER` 后刷新当前列表（今日订单页自动出现新单）

---

## 六、平台管理接口（`/api/admin/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`ADMIN`

### 6.1 仪表盘

#### 核心统计卡片

```
GET /api/admin/dashboard/stats
```

**响应 data：** `AdminDashboardVO`

```json
{
  "totalOrders": 156,
  "pendingApplications": 3,
  "totalMerchants": 8,
  "totalUsers": 13
}
```

---

#### 订单状态分布

```
GET /api/admin/dashboard/order-stats
```

**响应 data：** `List<OrderStatusStatVO>`

```json
[
  { "status": "PENDING_PAY", "label": "待支付", "count": 5 },
  { "status": "PAID", "label": "已支付", "count": 3 },
  { "status": "ACCEPTED", "label": "已接单", "count": 2 },
  { "status": "COOKING", "label": "制作中", "count": 4 },
  { "status": "COMPLETED", "label": "已完成", "count": 12 },
  { "status": "CANCELLED", "label": "已取消", "count": 1 }
]
```

---

#### 最新订单

```
GET /api/admin/dashboard/recent-orders?limit=10
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `limit` | int | 否 | 10 | 返回条数 |

**响应 data：** `List<AdminRecentOrderVO>`

---

### 6.2 商家入驻审核

#### 申请列表（支持关键词 + 状态 + 品类 + 日期筛选）

```
GET /api/admin/merchant-application/pending?page=1&size=20&keyword=&status=&categoryId=&dateStart=&dateEnd=
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 1 | 页码 |
| `size` | int | 否 | 20 | 每页条数 |
| `keyword` | string | 否 | - | 搜索店铺名/申请人 |
| `status` | string | 否 | - | 状态筛选：`PENDING` / `APPROVED` / `REJECTED` |
| `categoryId` | long | 否 | - | 主营品类筛选（`category` 表） |
| `dateStart` | string | 否 | - | 申请起始日期（`yyyy-MM-dd`） |
| `dateEnd` | string | 否 | - | 申请截止日期（`yyyy-MM-dd`） |

**响应 data：** `PageVO<AdminApplicationVO>`

```json
{
  "total": 5,
  "list": [
    {
      "id": 1,
      "shopName": "深夜食堂",
      "applicantUsername": "mbjsms8x5k",
      "phone": "138****1234",
      "logoUrl": "https://nsp.oss-cn-shenzhen.aliyuncs.com/nsp/2025-07/25/xxx.jpg",
      "categoryName": "中式快餐",
      "ownerName": "阿不都·热合曼",
      "address": "广东省广州市荔湾区荔湾广场店",
      "lat": 23.1287775,
      "lng": 113.2769936,
      "status": "PENDING",
      "rejectReason": null,
      "createdAt": "2025-07-18T21:30:00",
      "reviewedAt": null
    }
  ]
}
```

---

#### 申请详情

```
GET /api/admin/merchant-application/{id}/detail
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 申请 ID |

**响应 data：** `AdminApplicationVO`（含分类名称、申请人、地址坐标、拒绝原因、审核备注等完整字段）

---

#### 通过申请（V1）

```
PUT /api/admin/merchant-application/{id}/approve
```

> 商家状态 → `OPEN`

**可能错误码：** `3003`

---

#### 拒绝申请（V1）

```
PUT /api/admin/merchant-application/{id}/reject
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | string | 是 | 拒绝理由 |

> 申请状态 → `REJECTED`

**可能错误码：** `3003`

---

#### 通过申请（V2，推荐）

```
POST /api/admin/application/{id}/approve
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 申请 ID |

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `initialStatus` | string | 是 | 初始营业状态：`OPEN`(营业中) / `CLOSED`(打烊) |
| `reviewNote` | string | 否 | 审核备注（内部留存，≤200 字） |

> 配送半径不在审核时配置：商家审核通过后自行在店铺设置中调整（默认 5000 米）。

> 商家账号直接复用申请时填写的 `username` / `password`（见 3.5），凭据缺失（早期申请数据）时报 `1100`。

**可能错误码：** `3003`, `7007`, `1100`（申请缺少登录账号/密码）

---

#### 拒绝申请（V2，推荐）

```
POST /api/admin/application/{id}/reject
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 申请 ID |

**请求体：**

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| `reason` | string | 是 | @NotBlank，10–200 字 | 拒绝原因（将展示给商家） |

**可能错误码：** `3003`, `7001`, `7007`

---

### 6.3 用户管理

> 操作接口（封禁/解封/重置密码）均记录操作日志，写入 `user_operation_log` 表。

#### 用户列表（支持关键词 + 状态 + 注册日期筛选）

```
GET /api/admin/users?page=1&size=20&keyword=&status=&dateStart=&dateEnd=
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 搜索用户名/手机号 |
| `status` | string | 否 | 状态筛选：`ACTIVE` / `DISABLED` |
| `dateStart` | string | 否 | 注册起始日期（`yyyy-MM-dd`） |
| `dateEnd` | string | 否 | 注册截止日期（`yyyy-MM-dd`） |

**响应 data：** `PageVO<AdminUserVO>`（含订单数、消费金额，实时统计自 `order` 表；不含昵称）

```json
{
  "total": 128,
  "list": [
    {
      "id": 1,
      "username": "admin",
      "phone": "138****0001",
      "status": "ACTIVE",
      "lastLoginAt": "2026-08-08T10:00:00",
      "createdAt": "2026-01-05T00:00:00",
      "updatedAt": "2026-08-08T10:05:00"
    }
  ]
}
```

> 时间字段说明：`createdAt` 注册时间（数据库列默认 `CURRENT_TIMESTAMP`）、`lastLoginAt` 最后登录时间（登录成功后实时更新）、`updatedAt` 更新时间。均为 ISO-8601 T 格式。

---

#### 用户详情

```
GET /api/admin/users/{id}/detail
```

**响应 data：** `UserDetailVO`（用户完整信息 + 订单统计）

**可能错误码：** `2003`

---

#### 封禁用户（推荐）

```
PUT /api/admin/users/{id}/ban
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | string | 是 | 封禁原因（@NotBlank） |
| `durationDays` | int | 否 | 封禁时长（天），传 null 表示永久 |

> 状态 → `DISABLED`；写操作日志。

**可能错误码：** `2003`, `2302`（原因为空）, `2303`（已封禁）, `2309`（时长为负数）

---

#### 解封用户（推荐）

```
PUT /api/admin/users/{id}/unban
```

> 状态 → `ACTIVE`；写操作日志。

**可能错误码：** `2003`, `2304`（未封禁）

---

#### 封禁用户（旧版兼容）

```
PUT /api/admin/users/{id}/disable
```

> `deprecated` — 委托给 ban（原因固定为「管理员操作」，永久封禁），供旧前端使用。

**可能错误码：** `2003`, `2303`

---

#### 解封用户（旧版兼容）

```
PUT /api/admin/users/{id}/enable
```

> `deprecated` — 委托给 unban。

**可能错误码：** `2003`, `2304`

---

#### 操作日志列表

```
GET /api/admin/users/{id}/logs
```

**响应 data：** `List<Map<String, Object>>`（操作类型、操作人、时间、备注等）

**可能错误码：** `2003`

---

### 6.4 商家管理（V1）

#### 商家列表

```
GET /api/admin/merchants?page=1&size=20&keyword={keyword}&status={status}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 搜索店铺名/店主/电话 |
| `status` | string | 否 | `OPEN` / `CLOSED` / `SUSPENDED` / `PENDING` |

**响应 data：** `PageVO<AdminMerchantVO>`

```json
{
  "total": 15,
  "list": [
    {
      "id": 1,
      "name": "深夜食堂",
      "ownerName": "zhangsan",
      "phone": "138****0001",
      "logoUrl": "https://nsp.oss-cn-shenzhen.aliyuncs.com/nsp/2025-07/25/xxx.jpg",
      "categoryName": "中式快餐",
      "todayOrders": 12,
      "todayRevenue": 890.50,
      "avgRating": 4.8,
      "status": "OPEN",
      "lastLoginAt": "2025-07-23T18:30:00",
      "createdAt": "2025-07-15T00:00:00",
      "updatedAt": "2025-07-24T09:00:00"
    }
  ]
}
```

---

#### 封禁商家（V1）

```
PUT /api/admin/merchants/{id}/suspend
```

> 状态 → `SUSPENDED`

**可能错误码：** `3001`

---

#### 解封商家（V1）

```
PUT /api/admin/merchants/{id}/activate
```

> 状态 → `OPEN`

**可能错误码：** `3001`

---

### 6.5 商家管理（V2，推荐）

> 以下接口提供更细粒度的商家操作，含封禁原因校验和操作日志。

#### 商家统计概览

```
GET /api/admin/merchant/stats
```

**响应 data：** `MerchantStatsVO`

```json
{
  "total": 8,
  "openCount": 5,
  "pendingCount": 1,
  "suspendedCount": 1,
  "closedCount": 1
}
```

---

#### 商家详情

```
GET /api/admin/merchant/{id}/detail
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 商家 ID |

**响应 data：** `MerchantDetailVO`（含完整字段：店铺信息、店主、品类、评分、历史订单/今日订单/今日营收、营业时间、配送半径、简介、公告、封禁原因、入驻时间 `createdAt`、最后登录 `lastLoginAt`、修改时间 `updatedAt` 等）

---

#### 暂停营业

```
PUT /api/admin/merchant/{id}/pause
```

> 状态 → `CLOSED`（计入操作日志）

**可能错误码：** `3001`

---

#### 恢复营业

```
PUT /api/admin/merchant/{id}/resume
```

> 状态 → `OPEN`（计入操作日志）

**可能错误码：** `3001`

---

#### 封禁商家（V2）

```
PUT /api/admin/merchant/{id}/ban
```

**请求体：**

| 参数 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| `reason` | string | 是 | @NotBlank | 封禁原因 |

> 状态 → `SUSPENDED`，记录封禁原因、时间、操作人。计入操作日志。

**可能错误码：** `3001`, `7002`, `7003`

---

#### 解封商家（V2）

```
PUT /api/admin/merchant/{id}/unban
```

> 状态 → `OPEN`，清除封禁原因。计入操作日志。

**可能错误码：** `3001`, `7004`

---

#### 删除商家（软删）

```
DELETE /api/admin/merchant/{id}
```

> 软删除，数据不可恢复。计入操作日志。

**可能错误码：** `3001`

---

#### 操作日志列表

```
GET /api/admin/merchant/{id}/logs
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 商家 ID |

**响应 data：** `List<Map<String, Object>>`

```json
[
  {
    "time": "2025-07-23T18:30:00",
    "action": "封禁商家",
    "detail": "多次用户投诉，核实后封禁"
  },
  {
    "time": "2025-07-20T09:15:00",
    "action": "通过入驻申请",
    "detail": "初始状态: 营业中, 配送半径: 3000m"
  }
]
```

---

### 6.6 品类管理

> 平台级商家分类基础数据，管理员可 CRUD + 排序 + 启用/停用。

#### 品类列表（分页+搜索+状态筛选）

```
GET /api/admin/category?page=1&size=20&keyword={keyword}&status={status}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 1 | 页码 |
| `size` | int | 否 | 20 | 每页条数 |
| `keyword` | string | 否 | - | 搜索品类名（模糊匹配） |
| `status` | string | 否 | - | 状态筛选：`ENABLED` / `DISABLED` |

**响应 data：** `PageVO<CategoryVO>`

```json
{
  "total": 12,
  "list": [
    {
      "id": 1,
      "name": "烧烤类",
      "icon": "🍖",
      "iconType": "EMOJI",
      "sortOrder": 1,
      "status": "ENABLED",
      "remark": null,
      "createdAt": "2025-07-18T21:30:00",
      "updatedAt": "2025-07-20T14:00:00"
    }
  ]
}
```

---

#### 全部启用品类（下拉框用，无分页）

```
GET /api/admin/category/all
```

**响应 data：** `List<CategoryVO>`（仅 `ENABLED` 状态，按 `sortOrder` 升序）

---

#### 品类统计

```
GET /api/admin/category/stats
```

**响应 data：**

```json
{
  "total": 10,
  "enabled": 8,
  "disabled": 2
}
```

---

#### 品类详情

```
GET /api/admin/category/{id}
```

**可能错误码：** `8003`

---

#### 新增品类

```
POST /api/admin/category
```

**请求体：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | string | 是 | - | 品类名称（2-10 字） |
| `icon` | string | 否 | - | 图标（Emoji 或 OSS URL） |
| `iconType` | string | 否 | `EMOJI` | 图标类型：`EMOJI` / `OSS` |
| `sortOrder` | int | 否 | 最大值+1 | 排序权重（值越小越靠前） |
| `status` | string | 否 | `ENABLED` | 初始状态 |
| `remark` | string | 否 | - | 备注（≤200 字） |

> 校验：名称唯一、长度 2-10 字；sortOrder ≥ 0

**可能错误码：** `8001`, `8002`, `8006`

---

#### 修改品类

```
PUT /api/admin/category/{id}
```

**请求体：** 同新增（所有字段可选，传了才更新）

> **排序插入式重排**：修改 `sortOrder` 时，目标位置及其后的品类自动顺延。例如把排序 3 改为 4，原排序 4 及之后的品类依次变为 5、6、7...（后移区间 `(旧, 新]`、前移区间 `[新, 旧)` 内的品类排序 +1）。

**可能错误码：** `8001`, `8002`, `8003`, `8006`

---

#### 启用品类

```
PUT /api/admin/category/{id}/enable
```

> 状态 → `ENABLED`（幂等）

**可能错误码：** `8003`

---

#### 停用品类

```
PUT /api/admin/category/{id}/disable
```

> 状态 → `DISABLED`（幂等）。**前条件：无商家关联**（merchant.category_id + merchant_category 表均无记录），否则拒绝停用。

**可能错误码：** `8003`, `8005`

---

#### 删除品类（仅停用状态可删）

```
DELETE /api/admin/category/{id}
```

> 软删除。前条件：① 状态必须为 `DISABLED`；② 无商家关联（merchant.category_id + merchant_category 表均无记录）

**可能错误码：** `8003`, `8004`, `8005`

---

#### 批量更新排序

```
POST /api/admin/category/reorder
```

**请求体：** `List<CategoryReorderDTO>`

```json
[
  { "id": 1, "sortOrder": 3 },
  { "id": 2, "sortOrder": 1 },
  { "id": 3, "sortOrder": 2 }
]
```

**可能错误码：** `8003`, `8006`

---

### 6.7 订单管理

> 管理员可查看平台全部订单（含用户名/商家名），**不做状态流转操作**（接单/拒单由商家端完成）。

#### 订单列表

```
GET /api/admin/orders
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 1 |
| `size` | int | 否 | 每页条数，默认 20 |
| `keyword` | string | 否 | 订单号 / 下单用户名 / 商家名模糊搜索 |
| `status` | string | 否 | 状态筛选（见下方状态码表） |
| `payChannel` | string | 否 | 支付渠道筛选：`ALIPAY` / `WECHAT_MOCK` |
| `dateStart` | string | 否 | 下单起始日期（`yyyy-MM-dd`，含当天） |
| `dateEnd` | string | 否 | 下单截止日期（`yyyy-MM-dd`，含当天） |

**响应 data：** `PageVO<AdminOrderVO>`

```json
{
  "total": 28,
  "list": [
    {
      "id": 15,
      "orderNo": "NSP20260727015",
      "username": "liuer",
      "merchantName": "川味牛肉面馆",
      "status": "PAID",
      "totalAmount": 22.00,
      "payChannel": "WECHAT_MOCK",
      "createdAt": "2026-08-05T13:04:56"
    }
  ]
}
```

#### 订单详情

```
GET /api/admin/orders/{id}
```

**响应 data：** `OrderVO`（含 `items` 商品明细、`statusLogs` 状态流转、`addressSnapshot` 地址快照 JSON）

```json
{
  "id": 15,
  "orderNo": "NSP20260727015",
  "merchantName": "川味牛肉面馆",
  "status": "PAID",
  "totalAmount": 22.00,
  "addressSnapshot": "{\"receiver\":\"张二\",\"phone\":\"13800001116\",\"detail\":\"海淀区知春路56号\"}",
  "payChannel": "WECHAT_MOCK",
  "payTime": "2026-08-05T13:05:02",
  "items": [
    {
      "productId": 9,
      "specId": 25,
      "nameSnapshot": "招牌牛肉面-标准份",
      "priceSnapshot": 22.00,
      "quantity": 1,
      "imageUrl": "https://..."
    }
  ],
  "statusLogs": [
    { "fromStatus": null, "toStatus": "PENDING_PAY", "operator": "USER", "reason": "用户下单", "createdAt": "2026-01-22T02:36:44" }
  ],
  "createdAt": "2026-08-05T13:04:56"
}
```

#### 订单退款

```
POST /api/admin/orders/{id}/refund
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | long | 订单 ID |

**可退款状态规则：**

| 状态 | 行为 |
|------|------|
| `PAID` / `ACCEPTED` / `COOKING` | 可退款：`REFUNDING` → 渠道退款 → `REFUNDED` |
| `REFUNDING` | 上次退款失败，**重试**（复用同一退款请求号，渠道侧幂等） |
| `PENDING_PAY` | `4002`「订单未支付，无法退款」 |
| `CANCELLED` / `COMPLETED` / `REFUNDED` | `4002`「当前状态不允许退款」（售后不属本接口范围） |

> 支付宝沙箱发起真实退款（同步返回）；`WECHAT_MOCK` 走 mock 退款。沙箱中未真实支付的订单（渠道无交易）自动降级为本地标记退款，保证链路完整。退款失败保持 `REFUNDING`，可再次调用重试。

**响应 data：** `OrderVO`（退款完成后的订单，status=`REFUNDED`）

**可能错误码：** `4001`, `4002`, `5005`, `5007`

---

#### 批量退款

```
POST /api/admin/orders/batch-refund
```

**请求体：** `{ "ids": [1, 2, 3] }`（最多 100 条，见 1.5 批量契约）

> 逐条执行上表的退款规则：可退/可重试的退款，不可退的**跳过**并汇总原因（未支付/已完成/已退款/不存在），部分成功不算失败。

**响应 data：** `BatchResultVO`（`successCount` / `skippedCount` / `skippedReasons` / `message`）

**订单状态码表（`status` 与筛选共用）：**

| 状态码 | 中文 | 说明 |
|--------|------|------|
| `PENDING_PAY` | 待支付 | 下单未支付 |
| `PAID` | 已支付 | 支付成功待接单 |
| `ACCEPTED` | 已接单 | 商家已接单 |
| `COOKING` | 制作中 | 商家制作中 |
| `COMPLETED` | 已完成 | 制作完成送达 |
| `CANCELLED` | 已取消 | 用户取消 / 商家拒单 / 超时取消（拒单与超时取消自动退款） |
| `REFUNDING` | 退款中 | 管理员发起退款处理中（失败可重试） |
| `REFUNDED` | 已退款 | 退款完成 |

---

## 七、文件上传接口（`/api/upload/*`）

> 鉴权：`/api/upload/sign` **匿名**（商家入驻上传 LOGO 时尚未登录）；`bind` / `url` 需登录（`USER` / `MERCHANT` / `ADMIN`）；`callback` 匿名（OSS 服务端回调，验签）

### 7.1 获取 OSS 上传签名

```
GET /api/upload/sign?bizType={bizType}&targetId={targetId}&fileName={fileName}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `bizType` | string | 是 | 业务类型：`AVATAR` / `SHOP_LOGO` / `PRODUCT_IMAGE` |
| `targetId` | long | 否 | 关联业务对象 ID |
| `fileName` | string | 是 | 原始文件名（用于校验扩展名） |

**响应 data：**

```json
{
  "host": "https://nsp-upload.oss-cn-shenzhen.aliyuncs.com",
  "dir": "nsp/2025-07/25/",
  "policy": "eyJleHBpcmF...",
  "signature": "abc123...",
  "accessId": "LTAI5t...",
  "expire": 60
}
```

**OSS 文件命名规则**：`nsp/yyyy-MM/dd/{uuid}.{ext}`，按上传日期自动归类。

**可能错误码：** `6001`, `6002`

---

### 7.2 绑定上传文件

```
POST /api/upload/bind
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ossKey` | string | 是 | OSS 文件 Key |
| `bizType` | string | 是 | 业务类型 |
| `targetId` | long | 否 | 关联业务对象 ID |
| `url` | string | 是 | 文件访问 URL |

> 前端直传 OSS 成功后调用此接口，后端将文件记录落 `uploaded_file` 表。

---

### 7.3 获取文件临时访问 URL

```
GET /api/upload/url?ossKey={ossKey}
```

> 用于私有 Bucket，生成带签名的临时访问 URL。

---

### 7.4 OSS 上传回调（匿名）

```
POST /api/upload/callback
```

> 由阿里云 OSS 服务端回调，需校验回调签名防止伪造。
> 返回 `"success"` 或 `"fail"` 字符串。

**可能错误码：** `6004`

---

## 八、订单状态机

```
PENDING_PAY ──(支付成功)──→ PAID ──(商家接单)──→ ACCEPTED
     │                        │
     └──(超时/手动取消)──→ CANCELLED ←──(商家拒单 + 自动退款)──┘
                                          
ACCEPTED ──(开始制作)──→ COOKING ──(完成)──→ COMPLETED

PAID / ACCEPTED / COOKING ──(管理员退款)──→ REFUNDING ──(退款成功)──→ REFUNDED
                                              │
                                              └──(退款失败，保持 REFUNDING 可重试)
```

> 说明：商家拒单 / 超时未接单 → 订单 `CANCELLED` 并自动退款（payment_record → `REFUNDED`，订单不经过 `REFUNDING`）；`REFUNDING/REFUNDED` 是管理员主动退款的路径。

---

## 九、枚举值

| 枚举类型 | 值 | 说明 |
|----------|-----|------|
| **订单状态** | `PENDING_PAY` | 待支付 |
| | `PAID` | 已支付 |
| | `ACCEPTED` | 已接单 |
| | `COOKING` | 制作中 |
| | `COMPLETED` | 已完成 |
| | `CANCELLED` | 已取消 |
| | `REFUNDING` | 退款中 |
| | `REFUNDED` | 已退款 |
| **商家状态** | `PENDING` | 审核中 |
| | `OPEN` | 营业中 |
| | `CLOSED` | 已打烊 |
| | `SUSPENDED` | 已封禁 |
| **用户状态** | `ACTIVE` | 正常 |
| | `DISABLED` | 已封禁 |
| **支付渠道** | `ALIPAY` | 支付宝 |
| | `WECHAT_MOCK` | 微信模拟 |
| **商品状态** | `ON` | 上架 |
| | `OFF` | 下架 |
| **上传业务类型** | `AVATAR` | 头像 |
| | `SHOP_LOGO` | 店铺图 |
| | `PRODUCT_IMAGE` | 商品图 |
| **审核状态** | `PENDING` | 待审核 |
| | `APPROVED` | 已通过 |
| | `REJECTED` | 已拒绝 |

---

## 十、完整接口索引

| 序号 | 方法 | URL | 说明 | 鉴权 |
|------|------|-----|------|------|
| 1 | POST | `/api/user/register` | 用户注册 | 匿名 |
| 2 | POST | `/api/user/login` | 用户登录（用户专用） | 匿名 |
| 3 | POST | `/api/merchant/login` | 商家登录（商家专用） | 匿名 |
| 4 | POST | `/api/admin/login` | 管理员登录（管理员专用） | 匿名 |
| 5 | POST | `/api/merchant/register` | 商家入驻申请 | 匿名 |
| 6 | GET | `/api/merchant/category/all` | 平台启用品类 | 匿名 |
| 7 | POST | `/api/auth/refresh` | Token 刷新（双令牌轮换） | 匿名 |
| 8 | POST | `/api/pay/alipay/callback` | 支付宝回调 | 匿名 |
| 9 | GET | `/actuator/health` | 健康检查 | 匿名 |
| 10 | GET | `/api/user/info` | 获取当前用户信息 | 登录 |
| 11 | GET | `/api/user/address` | 地址列表 | USER |
| 12 | GET | `/api/user/address/{id}` | 地址详情 | USER |
| 13 | POST | `/api/user/address` | 新增地址 | USER |
| 14 | PUT | `/api/user/address/{id}` | 更新地址 | USER |
| 15 | DELETE | `/api/user/address/{id}` | 删除地址 | USER |
| 16 | PUT | `/api/user/address/{id}/default` | 设为默认地址 | USER |
| 17 | GET | `/api/user/cart` | 查询购物车 | USER |
| 18 | POST | `/api/user/cart` | 添加购物车商品 | USER |
| 19 | PUT | `/api/user/cart` | 更新购物车数量 | USER |
| 20 | DELETE | `/api/user/cart/{specId}` | 移除购物车商品 | USER |
| 21 | DELETE | `/api/user/cart` | 清空购物车 | USER |
| 22 | POST | `/api/user/order` | 创建订单（clientToken 幂等） | USER |
| 23 | GET | `/api/user/order` | 我的订单列表 | USER |
| 24 | GET | `/api/user/order/{id}` | 订单详情 | USER |
| 25 | POST | `/api/user/order/{id}/cancel` | 取消订单 | USER |
| 26 | POST | `/api/user/pay` | 发起支付 | USER |
| 27 | GET | `/api/user/pay/{orderNo}/status` | 支付状态查询 | USER |
| 28 | GET | `/api/merchant/list` | 浏览商家列表（分页） | 登录 |
| 29 | GET | `/api/merchant/{id}` | 商家详情 | 登录 |
| 30 | GET | `/api/merchant/shop` | 获取店铺信息（JWT 解析商家） | MERCHANT |
| 31 | PUT | `/api/merchant/shop` | 更新店铺信息（部分更新） | MERCHANT |
| 32 | PUT | `/api/merchant/shop/status` | 更新营业状态 | MERCHANT |
| 33 | GET | `/api/merchant/category` | 品类列表（JWT 解析商家） | MERCHANT |
| 34 | POST | `/api/merchant/category` | 新增品类 | MERCHANT |
| 35 | PUT | `/api/merchant/category/{id}` | 更新品类 | MERCHANT |
| 36 | DELETE | `/api/merchant/category/{id}` | 删除品类 | MERCHANT |
| 37 | GET | `/api/merchant/product/list` | 商品列表（保留 merchantId 参数） | 登录 |
| 38 | GET | `/api/merchant/product/{id}` | 商品详情 | 登录 |
| 39 | POST | `/api/merchant/product` | 新增商品 | MERCHANT |
| 40 | PUT | `/api/merchant/product/{id}` | 更新商品 | MERCHANT |
| 41 | PUT | `/api/merchant/product/{id}/status` | 上架/下架 | MERCHANT |
| 42 | POST | `/api/merchant/product/{productId}/spec` | 添加规格 | MERCHANT |
| 43 | DELETE | `/api/merchant/product/spec/{specId}` | 删除规格 | MERCHANT |
| 44 | DELETE | `/api/merchant/product/{id}` | 删除商品（停售才可删，级联删规格） | MERCHANT |
| 45 | GET | `/api/merchant/order` | 店铺订单列表（JWT 解析商家，支持状态/日期筛选） | MERCHANT |
| 46 | GET | `/api/merchant/order/stats` | 订单状态分布统计（tab 角标） | MERCHANT |
| 47 | WS | `/ws/merchant?token=` | 新订单实时推送（WebSocket，见 5.5） | MERCHANT |
| 48 | GET | `/api/merchant/order/{id}` | 订单详情 | MERCHANT |
| 49 | PUT | `/api/merchant/order/{id}/accept` | 接单 | MERCHANT |
| 50 | PUT | `/api/merchant/order/{id}/cooking` | 开始制作 | MERCHANT |
| 51 | PUT | `/api/merchant/order/{id}/complete` | 完成制作 | MERCHANT |
| 52 | PUT | `/api/merchant/order/{id}/reject` | 拒单（自动退款） | MERCHANT |
| 53 | GET | `/api/admin/dashboard/stats` | 核心统计卡片 | ADMIN |
| 54 | GET | `/api/admin/dashboard/order-stats` | 订单状态分布 | ADMIN |
| 55 | GET | `/api/admin/dashboard/recent-orders` | 最新订单 | ADMIN |
| 56 | GET | `/api/admin/merchant-application/pending` | 入驻申请列表 | ADMIN |
| 57 | GET | `/api/admin/merchant-application/{id}/detail` | 申请详情 | ADMIN |
| 58 | PUT | `/api/admin/merchant-application/{id}/approve` | 通过申请(V1) | ADMIN |
| 59 | PUT | `/api/admin/merchant-application/{id}/reject` | 拒绝申请(V1) | ADMIN |
| 60 | POST | `/api/admin/application/{id}/approve` | 通过申请(V2) | ADMIN |
| 61 | POST | `/api/admin/application/{id}/reject` | 拒绝申请(V2) | ADMIN |
| 62 | GET | `/api/admin/users` | 用户列表 | ADMIN |
| 63 | GET | `/api/admin/users/{id}/detail` | 用户详情 | ADMIN |
| 64 | PUT | `/api/admin/users/{id}/ban` | 封禁用户（推荐） | ADMIN |
| 65 | PUT | `/api/admin/users/{id}/unban` | 解封用户（推荐） | ADMIN |
| 66 | PUT | `/api/admin/users/{id}/disable` | 封禁用户（旧版兼容） | ADMIN |
| 67 | PUT | `/api/admin/users/{id}/enable` | 解封用户（旧版兼容） | ADMIN |
| 68 | GET | `/api/admin/users/{id}/logs` | 用户操作日志 | ADMIN |
| 69 | DELETE | `/api/admin/users/{id}` | 删除用户（物理删+级联） | ADMIN |
| 70 | POST | `/api/admin/users/batch-unban` | 批量启用用户 | ADMIN |
| 71 | POST | `/api/admin/users/batch-delete` | 批量删除用户（物理删+级联） | ADMIN |
| 72 | GET | `/api/admin/merchants` | 商家列表(V1) | ADMIN |
| 73 | PUT | `/api/admin/merchants/{id}/suspend` | 封禁商家(V1) | ADMIN |
| 74 | PUT | `/api/admin/merchants/{id}/activate` | 解封商家(V1) | ADMIN |
| 75 | GET | `/api/admin/merchant/stats` | 商家统计概览 | ADMIN |
| 76 | GET | `/api/admin/merchant/{id}/detail` | 商家详情(V2) | ADMIN |
| 77 | PUT | `/api/admin/merchant/{id}/pause` | 暂停营业 | ADMIN |
| 78 | PUT | `/api/admin/merchant/{id}/resume` | 恢复营业 | ADMIN |
| 79 | PUT | `/api/admin/merchant/{id}/ban` | 封禁商家(V2) | ADMIN |
| 80 | PUT | `/api/admin/merchant/{id}/unban` | 解封商家(V2) | ADMIN |
| 81 | DELETE | `/api/admin/merchant/{id}` | 删除商家 | ADMIN |
| 82 | POST | `/api/admin/merchant/batch-delete` | 批量删除商家（级联商品等） | ADMIN |
| 83 | GET | `/api/admin/merchant/{id}/logs` | 商家操作日志 | ADMIN |
| 84 | GET | `/api/admin/category` | 品类列表 | ADMIN |
| 85 | GET | `/api/admin/category/all` | 全部启用品类 | ADMIN |
| 86 | GET | `/api/admin/category/stats` | 品类统计 | ADMIN |
| 87 | GET | `/api/admin/category/{id}` | 品类详情 | ADMIN |
| 88 | POST | `/api/admin/category` | 新增品类 | ADMIN |
| 89 | PUT | `/api/admin/category/{id}` | 修改品类 | ADMIN |
| 90 | PUT | `/api/admin/category/{id}/enable` | 启用品类 | ADMIN |
| 91 | PUT | `/api/admin/category/{id}/disable` | 停用品类 | ADMIN |
| 92 | DELETE | `/api/admin/category/{id}` | 删除品类 | ADMIN |
| 93 | POST | `/api/admin/category/reorder` | 批量排序 | ADMIN |
| 94 | POST | `/api/admin/category/batch-enable` | 批量启用品类 | ADMIN |
| 95 | POST | `/api/admin/category/batch-disable` | 批量停用品类 | ADMIN |
| 96 | POST | `/api/admin/category/batch-delete` | 批量删除品类 | ADMIN |
| 97 | GET | `/api/admin/orders` | 订单列表 | ADMIN |
| 98 | GET | `/api/admin/orders/{id}` | 订单详情 | ADMIN |
| 99 | POST | `/api/admin/orders/{id}/refund` | 订单退款 | ADMIN |
| 100 | POST | `/api/admin/orders/batch-refund` | 批量退款 | ADMIN |
| 101 | POST | `/api/admin/orders/batch-delete` | 批量删除订单 | ADMIN |
| 102 | POST | `/api/admin/merchant-application/batch-approve` | 批量通过入驻申请 | ADMIN |
| 103 | POST | `/api/admin/merchant-application/batch-reject` | 批量拒绝入驻申请 | ADMIN |
| 104 | GET | `/api/upload/sign` | OSS 上传签名 | 登录 |
| 105 | POST | `/api/upload/bind` | 绑定上传文件 | 登录 |
| 106 | GET | `/api/upload/url` | 获取文件访问 URL | 登录 |
| 107 | POST | `/api/upload/callback` | OSS 上传回调 | 匿名 |

---

## 十一、测试账号（种子数据）

> Flyway 启动时自动插入，BCrypt 加密存储。
> 登录方式：**管理员**用用户名登录（`/api/admin/login`）；**用户**用手机号登录（`/api/user/login`）；**商家**用手机号登录（`/api/merchant/login`）。

### 管理员（2 人，用户名登录）

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `admin` | `admin123` | 平台管理员 |
| `superadmin` | `admin123` | 超级管理员 |

### 普通用户/顾客（13 人，手机号登录）

| 用户名 | 手机号 | 密码 | 状态 |
|--------|--------|------|------|
| `tester` | `13800000003` | `test123` | 正常（有地址 + 历史订单） |
| `zhangsan` | `13811111111` | `test123` | 正常 |
| `lisi` | `13822222222` | `test123` | 正常 |
| `wangwu` | `13833333333` | `test123` | 正常 |
| `zhaoliu` | `13844444444` | `test123` | 已封禁 |
| `sunqi` | `13855551111` | `test123` | 正常 |
| `zhouba` | `13866661112` | `test123` | 正常 |
| `wujiu` | `13877771113` | `test123` | 正常 |
| `zhengshi` | `13888881114` | `test123` | 正常 |
| `qianyi` | `13899991115` | `test123` | 正常 |
| `liuer` | `13800001116` | `test123` | 正常 |
| `caosan` | `13811111117` | `test123` | 正常 |
| `weisi` | `13822221118` | `test123` | 已封禁 |

### 商家（9 家，手机号登录）

| 用户名 | 手机号 | 密码 | 店铺 | 品类 | 营业时间 | 评分 | 状态 |
|--------|--------|------|------|------|----------|------|------|
| `nightbb` | `13800000002` | `m123456` | 深夜食堂 | 烧烤类 | 18:00-03:00 | 4.8 | 营业中 |
| `spicyhot` | `13855555555` | `m123456` | 辣翻天小龙虾 | 小龙虾 | 16:00-02:00 | 4.6 | 营业中 |
| `teahouse` | `13866666666` | `m123456` | 老王糖水铺 | 经典糖水 | 14:00-23:00 | 4.9 | 营业中 |
| `crispychick` | `13877778888` | `m123456` | 脆皮炸鸡（海淀总店） | 炸鸡套餐 | 10:00-22:00 | 4.5 | 营业中 |
| `hotnoodle` | `13888889999` | `m123456` | 川味牛肉面馆 | 面食类 | 08:00-21:00 | 4.3 | 营业中 |
| `teamilk` | `13866667777` | `m123456` | 元气奶茶实验室 | 招牌奶茶 | 09:00-22:00 | 4.7 | 营业中 |
| `pizzaking` | `13855556666` | `m123456` | 披萨大王 | 经典披萨 | 10:00-21:00 | 4.1 | 已打烊 |
| `malatang` | `13812345678` | `m123456` | 杨国福麻辣烫（望京店） | 自选麻辣烫 | 08:00-22:00 | 3.9 | 已封禁 |
| `zhangsan_bbq` | `13811111111` | `m123456` | 张三烧烤 | 烧烤类 | 16:00-01:00 | 5.0 | 营业中 |

---

## 十二、健康检查

```
GET /actuator/health
```

> 匿名可访问（运维探活 / pytest 服务就绪检查），仅暴露 health 端点（不暴露 env/beans 等敏感端点）。

**响应：**

```json
{ "status": "UP" }
```

> `status` 聚合数据库与 Redis 健康状态，任一组件不可用时返回 `DOWN`。
