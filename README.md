# 夜宵外卖平台（Night Snack Platform）

> 一个完整的 Spring Boot 外卖后端工程，服务于测开简历项目。

## 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.3.5 |
| ORM | MyBatis-Plus 3.5.8 |
| 安全 | Spring Security + JWT (HS256) |
| 数据库 | MySQL 8.x + Redis 5.x |
| 迁移 | Flyway 10.x |
| 文档 | SpringDoc OpenAPI (Swagger UI) |
| 对象存储 | 阿里云 OSS（服务端签名 + 前端直传） |
| 支付 | 支付宝沙箱 + 微信 Mock |
| 地图 | 百度地图 Web API + Haversine 降级 |
| 工具 | Hutool、Lombok、Jackson |

## 项目结构

```
night-snack-platform/
├── CLAUDE.md                     # 项目规范（Agent 必读）
├── README.md                     # 本文件
├── backend/                      # 后端 Maven 多模块工程
│   ├── pom.xml                   # 父 POM（统一版本管理）
│   ├── .gitignore
│   ├── .nsp-common/              # 公共模块（Result、异常、JWT、枚举）
│   ├── .nsp-pojo/                # 实体模块（Entity、DTO、VO）
│   └── .nsp-server/              # 启动模块（Controller、Service、Mapper）
│       ├── src/main/resources/
│       │   ├── application.yml           # 主配置（共用）
│       │   ├── application-dev.yml       # 开发配置（含密钥，gitignore）
│       │   ├── application-dev.yml.example  # 开发配置模板
│       │   ├── application-prod.yml      # 生产配置
│       │   ├── db/migration/             # Flyway 迁移脚本
│       │   └── lua/                      # Redis Lua 脚本
│       └── ...
├── frontend-user/                # 预留：用户端 Vue3 + Vant
├── frontend-merchant/            # 预留：商家后台 Vue3 + Element Plus
├── frontend-admin/               # 预留：管理后台 Vue3 + Element Plus
└── tests/                        # 预留：pytest 自动化测试
```

## 快速开始

### 前提条件

- JDK 17+
- Maven 3.6+
- MySQL 8.x
- Redis 5.x

### 1. 创建数据库

```sql
CREATE DATABASE nsp_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置开发环境

```bash
cd backend/.nsp-server/src/main/resources/

# 从模板复制开发配置文件
cp application-dev.yml.example application-dev.yml

# 编辑 application-dev.yml，填入你的真实配置：
#   - MySQL 用户名/密码
#   - Redis 密码/数据库编号
#   - 阿里云 OSS AK/SK（不配也能启动，只是上传功能不可用）
#   - 百度地图 AK（不配则用 Haversine 公式代替）
#   - 支付宝沙箱密钥（不配则支付功能不可用）
```

### 3. 启动

```bash
cd backend
mvn install -DskipTests
mvn spring-boot:run -pl .nsp-server
```

启动成功后访问：
- **Swagger UI**：http://localhost:8080/swagger-ui.html
- **API Docs JSON**：http://localhost:8080/v3/api-docs

### 4. 验证

```bash
# 注册新用户
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","phone":"13800138000"}'

# 登录获取 Token
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 查看附近的商家
curl http://localhost:8080/api/merchant/list?lat=39.996&lng=116.481
```

## 导入 Apifox

1. 启动应用后，访问 `http://localhost:8080/v3/api-docs` 复制全部 JSON
2. 打开 Apifox → **导入** → **URL 导入**
3. 填入 `http://localhost:8080/v3/api-docs` → 导入
4. 所有接口自动生成，按 Tag 分组：
   - 用户认证、用户信息、收货地址、购物车
   - 用户订单、商家订单
   - 商家店铺、品类管理、商品管理
   - 支付、支付回调、文件上传
   - 平台管理

## 测试账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员 |
| `nightbb` | `m123456` | 商家（深夜食堂店主） |
| `tester` | `test123` | 普通用户 |

> ⚠️ 种子数据的 BCrypt 哈希由 Flyway V2 脚本插入。如果密码不匹配，请用注册接口重新创建账号。

## 种子数据

启动后自动包含以下测试数据（`V2__seed_data.sql`）：

- **商家**：深夜食堂（北京望京 SOHO）
- **品类**：烧烤类、饮品类、主食类
- **商品**：羊肉串、牛肉串、烤鸡翅、酸梅汤、啤酒、炒方便面、烤馒头、烤茄子
- **规格**：每个商品 2-3 个规格，含价格增量和库存

## 配置说明

### 阿里云 OSS（图片上传）

在 `application-dev.yml` 中配置：

```yaml
aliyun:
  oss:
    endpoint: oss-cn-beijing.aliyuncs.com    # 地域节点
    bucket: your-bucket-name                 # Bucket 名称
    access-key-id: LTAI5xxxxxxxxx            # AccessKey ID
    access-key-secret: xxxxxxxxxxxxxx        # AccessKey Secret
    region: cn-beijing                       # 地域
    cdn-domain:                              # CDN 域名（可选）
    callback-url:                            # 回调 URL（可选）
```

上传流程：前端请求 `GET /api/upload/sign` → 获得签名 → 直传 OSS → 回调 `POST /api/upload/bind`

### 百度地图（距离计算）

```yaml
baidu:
  map:
    ak: your-baidu-ak    # 百度地图 AK（不配则用 Haversine 公式）
```

### 支付宝沙箱（支付）

1. 登录 [支付宝开放平台](https://open.alipay.com/) → 沙箱环境
2. 获取 APPID、应用私钥、支付宝公钥
3. 配置回调地址（本地开发用 ngrok 穿透）

```yaml
alipay:
  app-id: 90210001xxxxxx
  private-key: MIIEvAIBADANBgkqhki...
  alipay-public-key: MIIBIjANBgkqhki...
  notify-url: https://your-ngrok-url/api/pay/alipay/callback
```

### 微信支付（当前为 Mock）

无需配置，选择 `WECHAT_MOCK` 渠道直接返回支付成功。

## API 概览

| 角色 | 前缀 | 示例接口 |
|---|---|---|
| 用户 | `/api/user/**` | 注册、登录、地址 CRUD、购物车（Redis Hash）、下单、查单、支付 |
| 商家 | `/api/merchant/**` | 店铺管理、品类 CRUD、商品 CRUD、接单/拒单/完成 |
| 管理员 | `/api/admin/**` | 管理后台（Phase 2 占位） |
| 通用 | `/api/upload/**` `/api/pay/**` | OSS 上传签名、支付回调（匿名） |

### 用户端完整调用链

```
注册 → 登录(拿Token) → 浏览商家(/api/merchant/list)
  → 查看商品(/api/merchant/product/list)
  → 加购(/api/user/cart POST)
  → 新增地址(/api/user/address POST)
  → 下单(/api/user/order POST)
  → 支付(/api/user/pay POST)
  → 查订单(/api/user/order GET)
```

## 重要说明

- ⚠️ `application-dev.yml` 含真实密钥，已加入 `.gitignore`，不会被提交
- ✅ `application-dev.yml.example` 是模板文件，可安全提交
- 库存扣减使用 Redis Lua 原子操作，禁止 Java 先读后写
- 订单状态机：`PENDING_PAY → PAID → ACCEPTED → COOKING → COMPLETED`
- 配送范围以服务端 Haversine/百度地图计算为准
- 支付宝回调须验签 + 幂等（`payment_record.uk_order_no_channel`）
