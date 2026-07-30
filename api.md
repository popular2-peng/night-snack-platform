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

所有时间字段统一使用 ISO-8601 格式：`"2025-07-18T21:30:00"`

---

## 二、错误码

### 2.1 成功码

| code | 说明 |
|------|------|
| `0` | 请求成功 |

### 2.2 通用错误（1000 ~ 1999）

| code | 说明 |
|------|------|
| `1001` | 未登录 |
| `1002` | 无权限 |
| `1003` | Token 已过期 |
| `1004` | Token 无效 |
| `1100` | 参数校验失败 |
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

### 2.6 支付域（5000 ~ 5999）

| code | 说明 |
|------|------|
| `5001` | 支付订单不存在 |
| `5002` | 订单已支付（幂等） |
| `5003` | 支付签名校验失败 |
| `5004` | 支付渠道异常 |

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
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |
| `phone` | string | 是 | 手机号 |
| `role` | string | 否 | 角色，默认 `USER` |

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

**可能错误码：** `2001`, `2002`

---

### 3.2 用户登录

```
POST /api/user/login
```

> **跨表登录**：同一个接口依次查询 `admin` → `merchant` → `user` 三张表，匹配到即返回对应角色的 Token。无需区分登录入口。

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

**响应 data：**

```json
{
  "userId": 1,
  "username": "admin",
  "role": "ADMIN",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | long | 用户/管理员/商家 ID |
| `username` | string | 用户名 |
| `role` | string | 角色：`USER` / `MERCHANT` / `ADMIN` |
| `accessToken` | string | 访问令牌（有效期 2 小时） |
| `refreshToken` | string | 刷新令牌（有效期 7 天） |

**可能错误码：** `2003`, `2004`

---

### 3.3 支付宝支付回调（匿名）

```
POST /api/pay/alipay/callback
```

> 支付宝异步通知回调，由支付宝服务器发起。SecurityConfig 中已放行。
> 返回 `"success"` 或 `"fail"` 字符串（非 JSON）。

**可能错误码：** `5001`, `5003`

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
| `phone` | string | 是 | 收货电话 |
| `detail` | string | 是 | 详细地址 |
| `lat` | number | 是 | 纬度 |
| `lng` | number | 是 | 经度 |
| `isDefault` | boolean | 否 | 是否默认 |

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

> 购物车存储在 Redis Hash 中，Key: `cart:{user_id}`，TTL 7 天。

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

#### 创建订单（下单）

```
POST /api/user/order
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | long | 是 | 商家 ID |
| `addressId` | long | 是 | 收货地址 ID |
| `remark` | string | 否 | 备注 |
| `channel` | string | 是 | 支付渠道：`ALIPAY` / `WECHAT_MOCK` |

> 业务规则：校验商家营业状态 + 营业时间 + 配送范围（服务端 Haversine），Redis Lua 预扣库存。

**可能错误码：** `3002`, `3104`, `4003`, `4004`, `4005`

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
  "orderNo": "20250723...",
  "channel": "ALIPAY",
  "qrCode": "https://...",
  "status": "WAIT_PAY"
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

> 以下接口在商家端 Controller 定义，但用户端也可调用，权限标注为多角色。

#### 商家列表

```
GET /api/merchant/list?lat=39.99632&lng=116.48090
```

> 按距离排序（近→远），百度地图 API + Haversine 降级。

---

#### 商家详情

```
GET /api/merchant/{id}
```

**可能错误码：** `3001`

---

#### 商品列表

```
GET /api/merchant/product/list?merchantId={merchantId}&categoryId={categoryId}&page=1&size=20
```

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

### 5.1 店铺管理

#### 获取店铺信息

```
GET /api/merchant/shop
```

> 根据当前登录用户关联的商家返回。

---

#### 更新营业状态

```
PUT /api/merchant/shop/status?merchantId={merchantId}&status={status}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `merchantId` | long | 商家 ID |
| `status` | string | `OPEN`(营业中) / `CLOSED`(打烊) |

**可能错误码：** `3001`, `3301`

---

### 5.2 品类管理

#### 品类列表

```
GET /api/merchant/category?merchantId={merchantId}
```

---

#### 新增品类

```
POST /api/merchant/category?merchantId={merchantId}&name={name}&sort=0
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `merchantId` | long | 是 | - | 商家 ID |
| `name` | string | 是 | - | 品类名称 |
| `sort` | int | 否 | 0 | 排序 |

---

#### 更新品类

```
PUT /api/merchant/category/{id}?merchantId={merchantId}&name={name}&sort={sort}
```

**可能错误码：** `3201`

---

#### 删除品类

```
DELETE /api/merchant/category/{id}?merchantId={merchantId}
```

**可能错误码：** `3201`

---

### 5.3 商品管理

#### 新增商品

```
POST /api/merchant/product?merchantId={merchantId}
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

#### 更新商品

```
PUT /api/merchant/product/{id}?merchantId={merchantId}
```

**请求体：** 同新增

**可能错误码：** `3101`

---

#### 上架/下架

```
PUT /api/merchant/product/{id}/status?merchantId={merchantId}&status={status}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `ON`(上架) / `OFF`(下架) |

**可能错误码：** `3101`

---

#### 添加规格

```
POST /api/merchant/product/{productId}/spec?merchantId={merchantId}
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 规格名称（如"大杯/加珍珠"） |
| `priceDelta` | number | 是 | 价格差额（正数加价，0 基础价） |
| `stock` | int | 是 | 库存数量 |

**可能错误码：** `3101`

---

#### 删除规格

```
DELETE /api/merchant/product/spec/{specId}?merchantId={merchantId}
```

**可能错误码：** `3103`

---

### 5.4 订单管理

#### 店铺订单列表

```
GET /api/merchant/order?merchantId={merchantId}&page=1&size=20
```

**响应 data：** `PageVO<OrderListVO>`

---

#### 订单详情

```
GET /api/merchant/order/{id}
```

**可能错误码：** `4001`

---

#### 接单

```
PUT /api/merchant/order/{id}/accept?merchantId={merchantId}
```

> 状态迁移：`PAID` → `ACCEPTED`

**可能错误码：** `4001`, `4002`

---

#### 开始制作

```
PUT /api/merchant/order/{id}/cooking?merchantId={merchantId}
```

> 状态迁移：`ACCEPTED` → `COOKING`

**可能错误码：** `4001`, `4002`

---

#### 完成制作

```
PUT /api/merchant/order/{id}/complete?merchantId={merchantId}
```

> 状态迁移：`COOKING` → `COMPLETED`

**可能错误码：** `4001`, `4002`

---

#### 拒单

```
PUT /api/merchant/order/{id}/reject?merchantId={merchantId}&reason={reason}
```

> 状态迁移：`PAID` → `CANCELLED`（触发退款）

**可能错误码：** `4001`, `4002`

---

## 六、平台管理接口（`/api/admin/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`ADMIN`

### 6.1 仪表盘

> Dashboard 已拆分为 3 个独立接口，前端首页并行调用。

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

#### 申请列表（支持关键词 + 状态筛选）

```
GET /api/admin/merchant-application/pending?page=1&size=20&keyword=&status=
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 1 | 页码 |
| `size` | int | 否 | 20 | 每页条数 |
| `keyword` | string | 否 | - | 搜索店铺名/申请人 |
| `status` | string | 否 | - | 状态筛选：`PENDING` / `APPROVED` / `REJECTED` |

**响应 data：** `PageVO<AdminApplicationVO>`

```json
{
  "total": 5,
  "list": [
    {
      "id": 1,
      "shopName": "深夜食堂",
      "applicantUsername": "zhangsan",
      "phone": "138****1234",
      "logoUrl": "https://nsp.oss-cn-shenzhen.aliyuncs.com/nsp/2025-07/25/xxx.jpg",
      "categoryName": "中式快餐",
      "reason": "我有3年餐饮经验",
      "status": "PENDING",
      "rejectReason": null,
      "createdAt": "2025-07-18T21:30:00"
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

**响应 data：** `AdminApplicationVO`（含分类名称、申请原因、拒绝原因、审核备注等完整字段）

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
| `deliveryRadius` | int | 是 | 配送半径（米），范围 500–20000 |
| `reviewNote` | string | 否 | 审核备注（内部留存，≤200 字） |

**可能错误码：** `3003`, `7007`

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

#### 用户列表

```
GET /api/admin/users?page=1&size=20&keyword={keyword}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 搜索用户名/手机号 |

**响应 data：** `PageVO<AdminUserVO>`

```json
{
  "total": 128,
  "list": [
    {
      "id": 1,
      "username": "admin",
      "phone": "138****0001",
      "role": "ADMIN",
      "status": "ACTIVE",
      "createdAt": "2025-07-01T00:00:00"
    }
  ]
}
```

---

#### 封禁用户

```
PUT /api/admin/users/{id}/disable
```

> 状态 → `DISABLED`

**可能错误码：** `2003`

---

#### 解封用户

```
PUT /api/admin/users/{id}/enable
```

> 状态 → `ACTIVE`

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
      "createdAt": "2025-07-15T00:00:00"
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

**响应 data：** `MerchantDetailVO`（含完整字段：店铺信息、店主、品类、评分、历史订单/今日订单/今日营收、营业时间、配送半径、简介、公告、封禁原因、入驻时间、最后登录等）

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
      "createdAt": "2025-07-18 21:30",
      "updatedAt": "2025-07-20 14:00"
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

> 状态 → `DISABLED`（幂等）

**可能错误码：** `8003`

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

## 七、文件上传接口（`/api/upload/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`USER` / `MERCHANT` / `ADMIN`（登录即可）

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
     └──(超时/手动取消)──→ CANCELLED ←──(商家拒单)──┘
                                          
ACCEPTED ──(开始制作)──→ COOKING ──(完成)──→ COMPLETED

PAID ──(退款)──→ REFUNDING ──(退款成功)──→ REFUNDED
```

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
| 2 | POST | `/api/user/login` | 用户登录（跨表） | 匿名 |
| 3 | GET | `/api/user/info` | 获取当前用户信息 | 登录 |
| 4 | GET | `/api/user/address` | 地址列表 | USER |
| 5 | GET | `/api/user/address/{id}` | 地址详情 | USER |
| 6 | POST | `/api/user/address` | 新增地址 | USER |
| 7 | PUT | `/api/user/address/{id}` | 更新地址 | USER |
| 8 | DELETE | `/api/user/address/{id}` | 删除地址 | USER |
| 9 | PUT | `/api/user/address/{id}/default` | 设为默认地址 | USER |
| 10 | GET | `/api/user/cart` | 查询购物车 | USER |
| 11 | POST | `/api/user/cart` | 添加购物车商品 | USER |
| 12 | PUT | `/api/user/cart` | 更新购物车数量 | USER |
| 13 | DELETE | `/api/user/cart/{specId}` | 移除购物车商品 | USER |
| 14 | DELETE | `/api/user/cart` | 清空购物车 | USER |
| 15 | POST | `/api/user/order` | 创建订单 | USER |
| 16 | GET | `/api/user/order` | 我的订单列表 | USER |
| 17 | GET | `/api/user/order/{id}` | 订单详情 | USER |
| 18 | POST | `/api/user/order/{id}/cancel` | 取消订单 | USER |
| 19 | POST | `/api/user/pay` | 发起支付 | USER |
| 20 | GET | `/api/user/pay/{orderNo}/status` | 支付状态查询 | USER |
| 21 | POST | `/api/pay/alipay/callback` | 支付宝回调 | 匿名 |
| 22 | GET | `/api/merchant/list` | 浏览商家列表 | 登录 |
| 23 | GET | `/api/merchant/{id}` | 商家详情 | 登录 |
| 24 | GET | `/api/merchant/shop` | 获取店铺信息 | MERCHANT |
| 25 | PUT | `/api/merchant/shop/status` | 更新营业状态 | MERCHANT |
| 26 | GET | `/api/merchant/category` | 品类列表 | MERCHANT |
| 27 | POST | `/api/merchant/category` | 新增品类 | MERCHANT |
| 28 | PUT | `/api/merchant/category/{id}` | 更新品类 | MERCHANT |
| 29 | DELETE | `/api/merchant/category/{id}` | 删除品类 | MERCHANT |
| 30 | GET | `/api/merchant/product/list` | 商品列表 | 登录 |
| 31 | GET | `/api/merchant/product/{id}` | 商品详情 | 登录 |
| 32 | POST | `/api/merchant/product` | 新增商品 | MERCHANT |
| 33 | PUT | `/api/merchant/product/{id}` | 更新商品 | MERCHANT |
| 34 | PUT | `/api/merchant/product/{id}/status` | 上架/下架 | MERCHANT |
| 35 | POST | `/api/merchant/product/{productId}/spec` | 添加规格 | MERCHANT |
| 36 | DELETE | `/api/merchant/product/spec/{specId}` | 删除规格 | MERCHANT |
| 37 | GET | `/api/merchant/order` | 店铺订单列表 | MERCHANT |
| 38 | GET | `/api/merchant/order/{id}` | 订单详情 | MERCHANT |
| 39 | PUT | `/api/merchant/order/{id}/accept` | 接单 | MERCHANT |
| 40 | PUT | `/api/merchant/order/{id}/cooking` | 开始制作 | MERCHANT |
| 41 | PUT | `/api/merchant/order/{id}/complete` | 完成制作 | MERCHANT |
| 42 | PUT | `/api/merchant/order/{id}/reject` | 拒单 | MERCHANT |
| 43 | GET | `/api/admin/dashboard/stats` | 核心统计卡片 | ADMIN |
| 44 | GET | `/api/admin/dashboard/order-stats` | 订单状态分布 | ADMIN |
| 45 | GET | `/api/admin/dashboard/recent-orders` | 最新订单 | ADMIN |
| 46 | GET | `/api/admin/merchant-application/pending` | 入驻申请列表 | ADMIN |
| 47 | GET | `/api/admin/merchant-application/{id}/detail` | 申请详情 | ADMIN |
| 48 | PUT | `/api/admin/merchant-application/{id}/approve` | 通过申请(V1) | ADMIN |
| 49 | PUT | `/api/admin/merchant-application/{id}/reject` | 拒绝申请(V1) | ADMIN |
| 50 | POST | `/api/admin/application/{id}/approve` | 通过申请(V2) | ADMIN |
| 51 | POST | `/api/admin/application/{id}/reject` | 拒绝申请(V2) | ADMIN |
| 52 | GET | `/api/admin/users` | 用户列表 | ADMIN |
| 53 | PUT | `/api/admin/users/{id}/disable` | 封禁用户 | ADMIN |
| 54 | PUT | `/api/admin/users/{id}/enable` | 解封用户 | ADMIN |
| 55 | GET | `/api/admin/merchants` | 商家列表(V1) | ADMIN |
| 56 | PUT | `/api/admin/merchants/{id}/suspend` | 封禁商家(V1) | ADMIN |
| 57 | PUT | `/api/admin/merchants/{id}/activate` | 解封商家(V1) | ADMIN |
| 58 | GET | `/api/admin/merchant/stats` | 商家统计概览 | ADMIN |
| 59 | GET | `/api/admin/merchant/{id}/detail` | 商家详情(V2) | ADMIN |
| 60 | PUT | `/api/admin/merchant/{id}/pause` | 暂停营业 | ADMIN |
| 61 | PUT | `/api/admin/merchant/{id}/resume` | 恢复营业 | ADMIN |
| 62 | PUT | `/api/admin/merchant/{id}/ban` | 封禁商家(V2) | ADMIN |
| 63 | PUT | `/api/admin/merchant/{id}/unban` | 解封商家(V2) | ADMIN |
| 64 | DELETE | `/api/admin/merchant/{id}` | 删除商家 | ADMIN |
| 65 | GET | `/api/admin/merchant/{id}/logs` | 操作日志 | ADMIN |
| 66 | GET | `/api/admin/category` | 品类列表 | ADMIN |
| 67 | GET | `/api/admin/category/all` | 全部启用品类 | ADMIN |
| 68 | GET | `/api/admin/category/stats` | 品类统计 | ADMIN |
| 69 | GET | `/api/admin/category/{id}` | 品类详情 | ADMIN |
| 70 | POST | `/api/admin/category` | 新增品类 | ADMIN |
| 71 | PUT | `/api/admin/category/{id}` | 修改品类 | ADMIN |
| 72 | PUT | `/api/admin/category/{id}/enable` | 启用品类 | ADMIN |
| 73 | PUT | `/api/admin/category/{id}/disable` | 停用品类 | ADMIN |
| 74 | DELETE | `/api/admin/category/{id}` | 删除品类 | ADMIN |
| 75 | POST | `/api/admin/category/reorder` | 批量排序 | ADMIN |
| 76 | GET | `/api/upload/sign` | OSS 上传签名 | 登录 |
| 77 | POST | `/api/upload/bind` | 绑定上传文件 | 登录 |
| 78 | GET | `/api/upload/url` | 获取文件访问 URL | 登录 |
| 79 | POST | `/api/upload/callback` | OSS 上传回调 | 匿名 |

---

## 十一、测试账号（种子数据）

> Flyway 启动时自动插入，BCrypt 加密存储。

### 管理员（2 人）

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `admin` | `admin123` | 平台管理员 |
| `superadmin` | `admin123` | 超级管理员 |

### 普通用户/顾客（13 人）

| 用户名 | 密码 | 状态 |
|--------|------|------|
| `tester` | `test123` | 正常（有地址 + 历史订单） |
| `zhangsan` | `test123` | 正常 |
| `lisi` | `test123` | 正常 |
| `wangwu` | `test123` | 正常 |
| `zhaoliu` | `test123` | 已封禁 |
| `sunqi` | `test123` | 正常 |
| `zhouba` | `test123` | 正常 |
| `wujiu` | `test123` | 正常 |
| `zhengshi` | `test123` | 正常 |
| `qianyi` | `test123` | 正常 |
| `liuer` | `test123` | 正常 |
| `caosan` | `test123` | 正常 |
| `weisi` | `test123` | 已封禁 |

### 商家（8 家）

| 用户名 | 密码 | 店铺 | 品类 | 营业时间 | 评分 | 状态 |
|--------|------|------|------|----------|------|------|
| `nightbb` | `m123456` | 深夜食堂 | 烧烤类 | 18:00-03:00 | 4.8 | 营业中 |
| `spicyhot` | `m123456` | 辣翻天小龙虾 | 小龙虾 | 16:00-02:00 | 4.6 | 营业中 |
| `teahouse` | `m123456` | 老王糖水铺 | 经典糖水 | 14:00-23:00 | 4.9 | 营业中 |
| `crispychick` | `m123456` | 脆皮炸鸡（海淀总店） | 炸鸡套餐 | 10:00-22:00 | 4.5 | 营业中 |
| `hotnoodle` | `m123456` | 川味牛肉面馆 | 面食类 | 08:00-21:00 | 4.3 | 营业中 |
| `teamilk` | `m123456` | 元气奶茶实验室 | 招牌奶茶 | 09:00-22:00 | 4.7 | 营业中 |
| `pizzaking` | `m123456` | 披萨大王 | 经典披萨 | 10:00-21:00 | 4.1 | 已打烊 |
| `malatang` | `m123456` | 杨国福麻辣烫（望京店） | 自选麻辣烫 | 08:00-22:00 | 3.9 | 已封禁 |

> **注意**：数据库重建后密码生效。若重建前已启动过，需 `DROP TABLE flyway_schema_history` 并删库重建。
