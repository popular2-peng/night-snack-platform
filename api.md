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
  "accessToken": "eyJ...",
  "username": "zhangsan",
  "role": "USER"
}
```

**可能错误码：** `2001`, `2002`

---

### 3.2 用户登录

```
POST /api/user/login
```

**请求体：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

**响应 data：**

```json
{
  "accessToken": "eyJ...",
  "username": "admin",
  "role": "ADMIN"
}
```

**可能错误码：** `2003`, `2004`

---

### 3.3 支付宝支付回调（匿名）

```
POST /api/pay/alipay/callback
```

> 支付宝异步通知回调，由支付宝服务器发起。需在 `SecurityConfig` 中放行。
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
  "createdAt": "2025-07-01T00:00:00"
}
```

**可能错误码：** `2003`

---

### 4.3 收货地址

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

### 4.4 购物车

> 购物车存储在 Redis Hash 中，未登录不可使用。

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
| `merchantId` | long | 是 | 商家 ID |

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

### 4.5 订单

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

**可能错误码：** `4001`, `4002`

---

### 4.6 支付

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

### 4.7 公共浏览

> 以下接口在商家端定义，但用户端也可调用，权限标注为多角色。

#### 商家列表

```
GET /api/merchant/list?lat=39.99632&lng=116.48090
```

> 按距离排序（近→远），使用百度地图 API + Haversine 降级

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

> 根据当前登录用户关联的商家返回

---

#### 更新营业状态

```
PUT /api/merchant/shop/status?merchantId={merchantId}&status={status}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `OPEN`(营业中) / `CLOSED`(已打烊) |

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

> 状态：`PENDING_PAY`/`PAID` → `ACCEPTED`

**可能错误码：** `4001`, `4002`

---

#### 开始制作

```
PUT /api/merchant/order/{id}/cooking?merchantId={merchantId}
```

> 状态：`ACCEPTED` → `COOKING`

**可能错误码：** `4001`, `4002`

---

#### 完成制作

```
PUT /api/merchant/order/{id}/complete?merchantId={merchantId}
```

> 状态：`COOKING` → `COMPLETED`

**可能错误码：** `4001`, `4002`

---

#### 拒单

```
PUT /api/merchant/order/{id}/reject?merchantId={merchantId}&reason={reason}
```

> 状态：`PAID` → `CANCELLED`（触发退款）

**可能错误码：** `4001`, `4002`

---

## 六、平台管理接口（`/api/admin/*`）

> 鉴权：Header `Authorization: Bearer <token>`  
> 权限：`ADMIN`

### 6.1 首页统计

```
GET /api/admin/dashboard
```

**响应 data：**

```json
{
  "totalUsers": 128,
  "totalMerchants": 15,
  "todayOrders": 42,
  "todayRevenue": 3890.50
}
```

---

### 6.2 商家入驻审核

#### 待审核申请列表

```
GET /api/admin/merchant-application/pending?page=1&size=20
```

**响应 data：** `PageVO<AdminApplicationVO>`

```json
{
  "total": 5,
  "list": [
    {
      "id": 1,
      "shopName": "深夜食堂",
      "applicantUsername": "zhangsan",
      "reason": "我有3年餐饮经验",
      "status": "PENDING",
      "createdAt": "2025-07-18T21:30:00"
    }
  ]
}
```

---

#### 通过申请

```
PUT /api/admin/merchant-application/{id}/approve
```

> 商家状态 → `OPEN`

**可能错误码：** `3003`

---

#### 拒绝申请

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
      "phone": "13800138000",
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

### 6.4 商家管理

#### 商家列表

```
GET /api/admin/merchants?page=1&size=20&keyword={keyword}&status={status}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 否 | 搜索店铺名 |
| `status` | string | 否 | `OPEN` / `CLOSED` / `SUSPENDED` |

**响应 data：** `PageVO<AdminMerchantVO>`

```json
{
  "total": 15,
  "list": [
    {
      "id": 1,
      "name": "深夜食堂",
      "ownerName": "zhangsan",
      "phone": "13800138001",
      "status": "OPEN",
      "createdAt": "2025-07-15T00:00:00"
    }
  ]
}
```

---

#### 封禁商家

```
PUT /api/admin/merchants/{id}/suspend
```

> 状态 → `SUSPENDED`

**可能错误码：** `3001`

---

#### 解封商家

```
PUT /api/admin/merchants/{id}/activate
```

> 状态 → `OPEN`

**可能错误码：** `3001`

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
  "host": "https://nsp-upload.oss-cn-beijing.aliyuncs.com",
  "dir": "product/1/",
  "policy": "eyJleHBpcmF...",
  "signature": "abc123...",
  "accessId": "LTAI5t...",
  "expire": 60
}
```

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

> 前端直传 OSS 成功后调用此接口，后端将文件记录入库

---

### 7.3 获取文件临时访问 URL

```
GET /api/upload/url?ossKey={ossKey}
```

> 用于私有 Bucket，生成带签名的临时访问 URL

---

### 7.4 OSS 上传回调（匿名）

```
POST /api/upload/callback
```

> 由阿里云 OSS 服务端回调，需配置白名单 IP 校验。  
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
| **上传业务类型** | `AVATAR` | 头像 |
| | `SHOP_LOGO` | 店铺图 |
| | `PRODUCT_IMAGE` | 商品图 |
| **审核状态** | `PENDING` | 待审核 |
| | `APPROVED` | 已通过 |
| | `REJECTED` | 已拒绝 |
