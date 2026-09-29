# AGENTS.md — 夜宵外卖平台（nsp / night-snack-platform）

> 本文件供 AI Agent（Codex / Cursor / Copilot 等）阅读，用于理解项目背景、技术约束、编码规范与生成代码的边界。
> **Agent 在生成任何代码前，必须先读完本文件。**

---

## 一、项目定位

- **项目名称**：夜宵外卖平台（Night Snack Platform，缩写 `nsp`）
- **仓库名**：`night-snack-platform`（GitHub 私有，后续可转 Public）
- **目标用户**：一个平台管理员 + N 个商家 + 普通用户
- **终端形态**：
  - 用户端：移动端浏览器（Vue3 + Vant，H5）
  - 商家后台：桌面浏览器（Vue3 + Element Plus）
  - 平台管理后台：桌面浏览器（Vue3 + Element Plus）
- **核心业务**：用户浏览附近商家 → 选规格加购 → 下单 → 支付（支付宝沙箱 + 微信 mock）→ 商家接单制作 → 完成
- **项目灵魂（测试点富矿）**：
  - 鉴权三级（用户/商家/管理员），可做越权测试
  - 订单状态机（待支付→已支付→接单→制作中→完成 / 取消 / 退款）
  - 库存扣减防超卖（Redis + Lua）
  - 配送范围校验（百度地图 + 降级 Haversine）
  - 支付异步回调（支付宝验签 + 幂等）
  - 购物车（Redis Hash）
  - **图片上传（阿里云 OSS）**：商品图、店铺头像、用户头像，统一走 OSS 签名直传
- **业务素材来源**：作者本人家有夜宵餐饮店，营业时间、高峰期并发、打烊切换等场景为真实体感，非虚构

---

## 二、技术栈（已锁定，勿随意替换）

### 后端（Java 17 + Spring Boot 3.x）
- 构建：**Maven 多模块**（parent + 子模块，结构见第三节）
- 框架：Spring Boot 3.x、Spring Security（JWT）、MyBatis-Plus
- 数据库：MySQL 8.x（主业务）+ Redis 5.x（缓存/库存/购物车）
- 对象存储：**阿里云 OSS**（商品图/头像/店铺图，前端直传 + 服务端签名）
- 接口文档：SpringDoc OpenAPI（Swagger UI 自动生成，供 Apifox 导入）
- 工具库：Hutool、Lombok、Jackson、阿里云 OSS SDK
- 支付：alipay-sdk-java（沙箱）+ 自封装微信 mock 通道
- 地图：百度地图 Web 服务 API（HTTP 调用，AK 走环境变量）

### 前端（三个独立工程，可后续再建）
- 用户端：Vue3 + Vant 4 + Axios + Pinia
- 商家后台：Vue3 + Element Plus + Axios + Pinia
- 管理后台：Vue3 + Element Plus + Axios + Pinia

> 当前阶段只生成**后端多 Maven 工程**，前端与自动化测试工程暂不生成。

---

## 三、Maven 多模块结构（权威目录）

> ⚠️ **本节是目录结构的唯一真源（Single Source of Truth）。Agent 生成任何文件前必须先对照本节确认路径。**

```
night-snack-platform/
├── pom.xml                     # parent，统一版本管理
├── .nsp-common/                # 公共模块：工具类、异常、统一响应、JWT 工具、常量、枚举、OSS 工具
│   ├── pom.xml
│   └── src/main/java/com/nsp/common/
│       ├── result/   exception/   constant/   enum/
│       ├── jwt/      oss/        util/
│
├── .nsp-pojo/                  # 实体模块：entity / dto / vo（所有模块共享数据结构）
│   ├── pom.xml
│   └── src/main/java/com/nsp/pojo/
│       ├── entity/   dto/   vo/
│
└── .nsp-server/                # 业务启动模块（含 Spring Boot 启动类）
    ├── pom.xml
    ├── .gitignore              # 本模块单独忽略 .env / target / IDE 文件
    └── src/main/java/com/nsp/server/
        │
        ├── NsPpplication.java  # 启动类
        │
        ├── annotation/         # 自定义注解（@RequireRole 等）
        ├── aspect/             # 切面（日志、权限、防重）
        ├── config/             # 配置类（MyBatis-Plus / Redis / OSS / Swagger / WebMvc）
        ├── security/           # JWT 签发、过滤器、权限拦截
        ├── common/             # 本模块内部通用（ExceptionHandler、BaseController）
        │
        ├── controller/        # ⭐ Controller 按角色分层（见下方说明）
        │   ├── user/           #   用户端接口（浏览、购物车、订单、地址）
        │   ├── merchant/       #   商家后台接口（店铺、商品、接单）
        │   └── admin/          #   平台管理后台接口（审核、封禁、品类）
        │
        ├── service/            # ⭐ 业务接口（扁平，按业务域，不按角色分）
        │   ├── User{Service,ServiceImpl}.java
        │   ├── Address{Service,ServiceImpl}.java
        │   ├── Cart{Service,ServiceImpl}.java
        │   ├── Merchant{Service,ServiceImpl}.java
        │   ├── Product{Service,ServiceImpl}.java
        │   ├── Upload{Service,ServiceImpl}.java
        │   ├── Order{Service,ServiceImpl}.java
        │   ├── Payment{Service,ServiceImpl}.java
        │   ├── Map{Service,ServiceImpl}.java
        │   └── Admin{Service,ServiceImpl}.java
        │
        └── mapper/             # ⭐ 数据访问层（扁平，按业务域，不按角色分）
            ├── UserMapper.java
            ├── AddressMapper.java
            ├── CartMapper.java
            ├── MerchantMapper.java
            ├── ProductMapper.java
            ├── OrderMapper.java
            ├── PaymentMapper.java
            └── UploadedFileMapper.java
```

### 3.1 Controller 分层规则（重要）

**Controller 按「调用角色」分层**，对应三种前端入口：

| 目录 | URL 前缀 | 调用方 | 示例接口 |
|---|---|---|---|
| `controller/user/` | `/api/user/**`、`/api/cart/**`、`/api/order/**` | 移动端用户 | 注册、登录、浏览商家、加购、下单、查订单 |
| `controller/merchant/` | `/api/merchant/**`、`/api/m/order/**`、`/api/m/product/**` | 商家后台 | 店铺信息、商品 CRUD、规格、接单、拒单 |
| `controller/admin/` | `/api/admin/**` | 平台管理后台 | 商家审核、品类管理、用户封禁 |

**约束**：
- 一个 Controller 只能放在一个角色目录下，**禁止跨角色复用 Controller 类**
- 不同角色目录下的 Controller 可以**调用同一个 Service**（Service 不按角色分）
- 角色边界用 `@RequireRole("MERCHANT")` 等注解在 Controller 方法上声明

### 3.2 Service / Mapper 扁平化规则（重要）

**Service 和 Mapper 不按角色分层**，而是**按业务域扁平放置**，可被任意角色的 Controller 复用：

- ✅ `service/OrderService` 同时被 `controller/user/OrderController`（用户查自己订单）和 `controller/merchant/OrderController`（商家处理订单）调用
- ✅ `mapper/ProductMapper` 同时被商家后台（商品 CRUD）和用户端（浏览商品）使用
- ❌ 不要建 `service/user/`、`service/merchant/` 这种按角色再分层的结构
- ❌ 不要建 `mapper/product/MerchantProductMapper` 和 `mapper/product/UserProductMapper` 这种重复 Mapper

**理由**：业务规则只有一份（如下单扣库存、状态机迁移），按角色拆分会复制逻辑、产生不一致。角色差异体现在 Controller 的 URL 前缀和权限注解上，不体现在 Service/Mapper 上。

### 3.3 模块依赖原则

- `.nsp-common` 被所有模块依赖（工具、异常、Result、JWT 工具、OSS 工具）
- `.nsp-pojo` 被所有模块依赖（entity / dto / vo）
- `.nsp-server` 依赖 `.nsp-common` 和 `.nsp-pojo`
- Service / Mapper 之间**允许互相调用**（扁平结构下的正常复用），但禁止循环依赖

---

## 四、数据库设计（核心表，MySQL）

> 表名统一小写 + 下划线；主键 `BIGINT AUTO_INCREMENT`；时间字段统一 `DATETIME DEFAULT CURRENT_TIMESTAMP`；软删字段 `deleted TINYINT DEFAULT 0`。
> 图片字段统一存 **OSS 访问 URL**（不要存二进制）。

### 用户域
- `user`（id, username, password, phone, role, avatar_url, created_at）
- `user_address`（id, user_id, receiver, phone, detail, lat, lng, is_default）
- `cart_item`（Redis Hash，不落 MySQL：`cart:{user_id}` → {sku_id: qty}）

### 商家域
- `merchant`（id, name, owner_user_id, phone, address, lat, lng, avatar_url, status 营业中/打烊/审核中, business_hours_start, business_hours_end, delivery_radius）
- `category`（id, merchant_id, name, sort）
- `product`（id, merchant_id, category_id, name, price, image_url, status 上架/下架, description）
- `product_spec`（id, product_id, name 如"大杯/加珍珠", price_delta, stock）

### 订单域
- `order`（id, order_no, user_id, merchant_id, status, total_amount, address_snapshot, pay_channel, pay_time, created_at）
- `order_item`（id, order_id, product_id, spec_id, name_snapshot, price_snapshot, quantity）
- `order_status_log`（id, order_id, from_status, to_status, operator, reason, created_at）# 状态机审计

### 支付域
- `payment_record`（id, order_no, channel alipay/wechat, trade_no, amount, status, notify_count, last_notify_at）

### 上传域
- `uploaded_file`（id, biz_type 商品图/头像/店铺图, target_id, oss_key, url, uploader_id, created_at）

### 管理域
- `merchant_application`（id, applicant_user_id, shop_name, reason, status 待审/通过/拒绝, reviewer_id, reviewed_at）

### 枚举/字典（不另建表，用 Java Enum）
- 订单状态：`PENDING_PAY / PAID / ACCEPTED / COOKING / COMPLETED / CANCELLED / REFUNDING / REFUNDED`
- 商家状态：`PENDING / OPEN / CLOSED / SUSPENDED`
- 支付渠道：`ALIPAY / WECHAT_MOCK`
- 上传业务类型：`AVATAR / SHOP_LOGO / PRODUCT_IMAGE`

---

## 五、阿里云 OSS 接入规范（图片上传）

> 所有图片（商品图、店铺头像、用户头像）**不存服务器本地磁盘**，统一走阿里云 OSS。

### 5.1 推荐方案：服务端签名 + 前端直传

- 前端请求后端 `/api/upload/sign`
- 后端用 OSS SDK 生成**签名**（policy + signature + host + dir），返回给前端
- 前端直接用签名**直传 OSS**，不经过后端流量
- 上传成功后，前端把 OSS key + 访问 URL 通过 `/api/upload/bind` 回写给后端，后端落 `uploaded_file` 表并关联到业务对象

### 5.2 后端关键接口（Controller 放 `controller/user/` 或 `controller/merchant/`，Service 用 `UploadService`）

| 接口 | 方法 | 鉴权 | 说明 |
|---|---|---|---|
| `/api/upload/sign` | GET | 登录即可 | 返回 OSS 上传签名（policy + signature + host + dir） |
| `/api/upload/sts` | GET | 登录即可 | 返回 STS 临时凭证（可选方案） |
| `/api/upload/bind` | POST | 登录即可 | 前端传 ossKey + 业务类型 + 业务ID，落库关联 |
| `/api/upload/url` | GET | 登录即可 | 给 ossKey 返回带签名的临时访问 URL（私有桶用） |
| `/api/upload/callback` | POST | 匿名（白名单 IP） | OSS 上传完成回调，校验签名后落库 |

### 5.3 配置（走环境变量，不写死）

```yaml
aliyun:
  oss:
    endpoint: ${ALIYUN_OSS_ENDPOINT:oss-cn-shenzhen.aliyuncs.com}
    bucket: ${ALIYUN_OSS_BUCKET:nsp-upload}
    access-key-id: ${ALIYUN_OSS_AK:placeholder}
    access-key-secret: ${ALIYUN_OSS_SK:placeholder}
    region: ${ALIYUN_OSS_REGION:shenzhen}
    cdn-domain: ${ALIYUN_OSS_CDN:placeholder}
    callback-url: ${ALIYUN_OSS_CALLBACK:placeholder}
```

### 5.4 安全约束

- Bucket 设为**私有**（不公开读），访问走签名 URL 或 CDN 鉴权
- 上传目录按业务分：`avatar/{userId}/`、`shop/{shopId}/`、`product/{productId}/`
- 文件大小限制（如单图 ≤ 5MB）、类型白名单（`image/jpeg`, `image/png`, `image/webp`）
- 防刷：同一用户单位时间内签名次数限流（Redis 计数）
- 回调接口必须**校验 OSS 回调签名**，防止伪造

### 5.5 测试点（测开视角，简历可写）

> "图片上传采用阿里云 OSS 服务端签名 + 前端直传方案，Bucket 私有、CDN 鉴权访问；后端提供签名/STS 接口并做限流，OSS 回调校验签名后落库关联业务对象。Pytest 中对签名接口、回调校验、类型/大小拦截、限流均覆盖用例。"

---

## 六、接口设计规范（Agent 生成 Controller 必须遵守）

1. **URL 风格**：RESTful，资源复数，模块前缀
   - 用户端：`/api/user/**`、`/api/cart/**`、`/api/order/**`
   - 商家端：`/api/merchant/**`、`/api/m/order/**`、`/api/m/product/**`
   - 管理端：`/api/admin/**`
   - 上传：`/api/upload/**`
   - 支付：`/api/pay/**`、`/api/pay/alipay/callback`（匿名，白名单）
2. **统一响应体**：`Result<T> { code: int, msg: string, data: T }`，业务码非 HTTP 状态码
3. **鉴权**：除登录/注册/支付宝回调/OSS 回调外，全部走 JWT（Header `Authorization: Bearer <token>`）
4. **权限注解**：自定义 `@RequireRole("MERCHANT")` / `@RequireRole("ADMIN")`，网关层拦截
5. **参数校验**：用 Jakarta Validation（`@NotBlank`、`@Min`），Controller 入口即校验
6. **文件上传**：不接受 `multipart/form-data` 走后端磁盘，统一走 OSS 直传链路
7. **异常**：统一 `@ControllerAdvice` 捕获，返回 `Result` 结构，不抛堆栈给前端
8. **分页**：统一 `?page=1&size=20`，响应含 `total / list`
9. **文档**：每个 Controller 加 SpringDoc 注解（`@Operation`、`@Schema`），保证 Swagger UI 可访问
10. **Controller 位置**：严格按第三节 3.1 放进 `controller/user|merchant|admin/` 对应子目录

---

## 七、Redis 使用约定

| Key 模式 | 类型 | 用途 | TTL |
|---|---|---|---|
| `cart:{user_id}` | Hash (sku_id → qty) | 用户购物车 | 7 天 |
| `product:stock:{sku_id}` | String (int) | 商品库存（扣减用） | 永久 |
| `stock:lock:{order_id}` | String | 下单预扣库存锁 | 30 分钟（订单超时释放） |
| `token:blacklist:{jti}` | Set | 登出/改密后 JWT 拉黑 | 与 token 过期一致 |
| `shop:distance:cache:{shop}_{user}` | String (int 米) | 地图距离缓存 | 1 小时 |
| `upload:sign:limit:{user_id}` | String (int) | 上传签名限流计数 | 1 分钟 |

> **库存扣减必须用 Lua 脚本保证原子性**，禁止先读再写的 Java 代码扣库存。

---

## 八、编码与工程规范（Agent 必须遵守）

- **Java 版本**：17，使用 `record` / `sealed class` / `switch` 表达式等现代语法
- **包结构**：每个模块 `com.nsp.{模块}.{子包}`（如 `com.nsp.server.controller.user`）
- **分层**：Controller 只做参数校验 + 调 Service；Service 写业务；Mapper 只做 SQL
- **DTO / VO 分离**：入参 DTO，出参 VO，禁止把 Entity 直接返回前端
- **日志**：SLF4J + Logback，关键链路（下单/支付回调/上传回调）打 INFO，含 `orderNo` / `userId` / `ossKey`
- **配置**：`application.yml` 按 `dev / test / prod` 三套，`application-prod.yml` 不提交真实密钥
- **敏感配置**：百度地图 AK、支付宝公私钥、JWT 密钥、阿里云 OSS AK/SK → 走环境变量或 `.env`（`.env` 不入库）
- **不要生成**：Dockerfile、K8s yaml、CI 配置（除非用户明确要求）
- **不要过度设计**：MVP 阶段不引入消息队列、不分库分表、不做读写分离

---

## 九、🔒 安全红线（最高优先级，违反即视为严重错误）

> **本节是 Agent 生成代码时的最高优先级约束。任何违反本条的产出必须被撤回。**

### 9.1 严禁提交进 Git 的内容

以下任何一项**不得出现在任何提交中**（包括 .yml / .properties / 代码 / 注释 / commit message）：

| 类别 | 具体值 | 正确做法 |
|---|---|---|
| 阿里云 OSS | `access-key-id`、`access-key-secret` | 走环境变量 `${ALIYUN_OSS_AK}`，`.env` 加入 `.gitignore` |
| 百度地图 | 地图服务 AK | 走环境变量 `${BAIDU_MAP_AK}`，`.env` 加入 `.gitignore` |
| 支付宝沙箱 | APPID、应用私钥、支付宝公钥 | 走环境变量 `${ALIPAY_APP_ID}` 等，`.env` 加入 `.gitignore` |
| JWT | 签名密钥（secret / jti 盐） | 走环境变量 `${JWT_SECRET}`，`.env` 加入 `.gitignore` |
| 数据库 | 生产库 URL、用户名、密码 | 走环境变量或本地 `application-dev.yml`（不提交 prod 配置） |
| 任何 `.env` 文件 | 任何形式 | **必须**在根目录和 `.nsp-server/` 的 `.gitignore` 中忽略 |
| IDE 配置 | `.idea/`、`.vscode/`、`.iml` | 已在 `.gitignore` |
| 编译产物 | `target/`、`.class`、`*.jar` | 已在 `.gitignore` |

### 9.2 `.gitignore` 强制要求

项目根目录和 `.nsp-server/` 下**必须存在 `.gitignore`**，至少包含：

```
# 环境变量（含所有密钥）
.env
.env.*
!.env.example

# IDE
.idea/
.vscode/
*.iml

# 编译产物
target/
*.class
*.jar
*.war

# 日志
*.log
logs/

# OS
.DS_Store
Thumbs.db
```

### 9.3 代码中的禁止行为

- ❌ `private static final String AK = "LTAI5t...";` （硬编码密钥）
- ❌ `ak: LTAI5t...` 写在 `application.yml`（即使是 dev 也不行，防止误提交）
- ❌ 在注释里写"测试用密钥：xxx"
- ❌ commit message 写"fix: 改了密钥为 xxx"
- ✅ 所有密钥只通过 `@Value("${ALIYUN_OSS_AK}")` 或 `System.getenv()` 注入
- ✅ 提供 `.env.example` 模板文件（值全为 `placeholder`），方便他人复制为 `.env` 自行填写

### 9.4 Agent 自检义务

每次生成涉及配置、密钥、yml 的代码时，Agent **必须自检**：
1. 这个文件里有没有任何看起来像真实密钥的字符串？（长度 > 20 的随机字符、Base64 串、以 `LTAI` 开头等）
2. 如果有 → **立即停止**，改为环境变量占位符
3. 生成完 yml / properties 后，主动提示用户："请确认 `.env` 已加入 `.gitignore`"

---

## 十、安全约束（业务层）

1. **密码**：BCrypt 加密，禁止明文/MD5
2. **JWT**：HS256，有效期 2 小时，刷新 token 7 天，登出进黑名单
3. **越权防护**：
   - 商家只能查自己店铺的订单（SQL 必须带 `merchant_id = 当前商家`）
   - 用户只能查自己的订单
   - 管理员的接口单独前缀 + 强校验
4. **输入校验**：所有外部入参走 Validation，防止 SQL 注入 / XSS
5. **支付回调**：必须验签，同一 `out_trade_no` 只处理一次（幂等，查 `payment_record`）
6. **地图 AK**：服务端调用，AK 不出现在前端代码
7. **OSS**：Bucket 私有、上传签名/STS 限时、回调验签、类型大小白名单、限流

---

## 十一、业务规则（Agent 写 Service 时必须遵守）

1. **配送范围**：商家有 `lat/lng` 和 `delivery_radius`（米），用户下单时服务端算距，**前端传的坐标只作参考，以服务端为准**
2. **营业时间**：下单时校验商家 `status=OPEN` 且当前时间在 `business_hours` 内，否则拒绝
3. **库存扣减顺序**：下单时 Lua 预扣 → 支付成功后正式扣减 → 订单取消/超时释放
4. **订单状态机**：只允许合法迁移，非法迁移抛 `IllegalState` 并记录日志
5. **购物车**：未登录不能加购；切换店铺时提示"是否清空其他店铺商品"（跨店禁止合并下单）
6. **支付渠道**：支付宝走沙箱 precreate；微信走 mock 接口直接成功（但保留可替换为正式 JSAPI 的接口抽象）
7. **图片上传**：商品图/头像必须先传 OSS 拿到 URL，再随业务接口提交（不接收 base64 大字段）

---

## 十二、给 Agent 的明确边界（Do / Don't）

✅ **DO**
- 生成代码前先确认模块归属（这个接口该放哪个 service / 哪个 controller 子目录）
- 每个接口同时生成：Controller + Service 接口 + Service 实现 + Mapper + DTO + VO + 单元测试骨架
- 关键业务方法写 JavaDoc，说明业务规则与异常
- 数据库表先建 migration（用 Flyway 或纯 SQL 文件放 `db/migration`），不要靠 JPA 自动建表
- 涉及金额/库存/状态的代码，主动提示"这里需要事务 / 需要幂等 / 需要加锁"
- OSS 相关代码 Service 用 `UploadService`，Controller 按需放 user/merchant 目录
- 涉及文件/图片的返回，统一返回 OSS URL，不返回二进制流
- 每次生成涉及配置的代码后，主动检查是否违反第九节安全红线

❌ **DON'T**
- 不要生成前端代码（本阶段）
- 不要生成自动化测试工程（pytest 项目由用户单独维护）
- 不要引入 RabbitMQ / Kafka / Redis Stream 等消息队列
- 不要做分布式事务（Seata 等），MVP 用本地事务即可
- 不要写"TODO 后续完善"就结束，要么实现要么明确标注 `@Deprecated` 并说明原因
- 不要把百度地图 AK、支付宝密钥、阿里云 OSS AK/SK 硬编码进代码或 yml
- 不要接受前端传二进制文件落服务器磁盘（必须用 OSS 直传链路）
- 不要把 Service / Mapper 按角色（user/merchant/admin）再分层——Service 和 Mapper 是扁平的，按业务域命名

---

## 十三、本次需要 Agent 优先生成的模块顺序

> 用户当前阶段目标：先把**后端骨架 + 核心业务跑通**，前端与测试工程后置。

**Phase 1（先生成这个）**
1. parent `pom.xml`（统一 Spring Boot / MyBatis-Plus / Hutool / Lombok / 阿里云 OSS SDK 版本）
2. `.nsp-common`：统一响应 `Result`、业务异常 `BizException`、JWT 工具、OSS 工具类、常量、枚举
3. `.nsp-pojo`：`entity`（User/Merchant/Product/Order/UploadedFile…）、`dto`、`vo`
4. `.nsp-server` 骨架：
   - `NsPpplication.java`、config（MyBatis-Plus/Redis/Swagger/WebMvc/OSS）、security（JWT 签发 + 拦截器）、exception handler
   - `.gitignore`（含 `.env` 忽略规则，见第九节）
   - `.env.example` 模板文件
5. `controller/user/`、`controller/merchant/`、`controller/admin/` 三个目录各放一个示例 Controller（如 `UserController`、`MerchantController`、`AdminController`），验证分层结构
6. `service/` + `mapper/` 扁平结构：先建 `UserService` + `UserMapper` 等核心几个
7. `upload` 相关：`UploadService` + `UploadedFileMapper` + 上传接口（放 `controller/user/` 或 `controller/merchant/`）

**Phase 2（验证完 Phase 1 再生成）**
8. `cart` 相关：Redis 购物车
9. `order` 相关：下单（含库存预扣 Lua）、状态机、取消、超时释放
10. `payment` 相关：支付宝沙箱 precreate + 回调验签 + 幂等；微信 mock 通道
11. `map` 相关：百度地图地理编码 + 距离计算 + Haversine 降级
12. `admin` 相关：商家入驻审核、品类管理、用户封禁

**Phase 3（最后）**
13. 补全 Flyway SQL 初始化脚本
15. 生成 `README.md` 说明如何启动、如何导入 Swagger 到 Apifox、如何配置 OSS / 百度 AK / 支付宝沙箱

---

## 十四、用户背景（供 Agent 理解"为什么这么设计"）

- 用户是软件工程 25 届毕业生，毕业后帮家里夜宵店忙一年多，正在自学测开（pytest + requests + yaml + apifox）
- 本项目是**为测开简历服务的后端**，不是为做全栈
- 因此：**接口设计要便于自动化测试**（参数清晰、响应稳定、错误码规范），不要为了炫技加无意义复杂度
- 用户 Java 能力中等，Agent 生成的代码要**可读、注释充分**，避免重度函数式/反射/字节码技巧
- 用户熟悉 Python 测开栈，所以后端错误码、JSON 结构要**对 pytest 友好**（稳定字段名、明确 code/msg/data）
- 用户已有 GitHub 私有仓库 `night-snack-platform`，代码将提交至此
- 用户高度重视密钥安全，**任何密钥泄漏进 git 都是不可接受的**

---

## 十五、与测试项目的接口约定（提前留好）

> 这一段是为后续用户单独建 pytest 项目时准备的，Agent 生成后端时请遵守，方便未来对接。

- 所有接口响应统一为 `{ "code": 0, "msg": "ok", "data": {...} }`，`code=0` 表示业务成功
- 分页统一字段：`{ "code":0, "msg":"ok", "data": { "total": 100, "list": [...] } }`
- 时间字段统一 ISO-8601 字符串（如 `2025-07-18T21:30:00`）
- Token 统一放 Header `Authorization: Bearer <jwt>`
- 错误码预留段：`1000-1999` 通用 / `2000-2999` 用户 / `3000-3999` 商品 / `4000-4999` 订单 / `5000-5999` 支付 / `6000-6999` 上传
- Swagger UI 地址：`http://localhost:8080/swagger-ui.html`，确保匿名可访问以便 Apifox 导入
- 上传接口返回 OSS URL，测试时可直接用 URL 断言图片可访问
