# 夜宵外卖平台（Night Snack Platform）

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white)
![pytest](https://img.shields.io/badge/pytest-8.x-0A9EDC?logo=pytest&logoColor=white)

> 面向夜宵餐饮场景的前后端分离外卖平台：用户端 H5、商家后台、平台管理后台 + Spring Boot 后端，附完整接口契约与 pytest 自动化测试工程。

---

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [默认账号](#默认账号)
- [API 文档](#api-文档)
- [自动化测试](#自动化测试)
- [部署](#部署)
- [配置说明](#配置说明)
- [错误码规范](#错误码规范)
- [安全说明](#安全说明)
- [常见问题](#常见问题)
- [相关文档](#相关文档)
- [许可证](#许可证)

---

## 项目简介

夜宵外卖平台是一套覆盖「用户下单 → 商家接单制作 → 平台监管」完整业务闭环的外卖系统，包含四个可独立运行的工程：

| 工程 | 说明 |
|---|---|
| `backend/` | Java 17 + Spring Boot 3 后端，Maven 多模块 |
| `frontend-user/` | 用户端 H5（Vue 3 + Vant） |
| `frontend-merchant/` | 商家后台（Vue 3 + Element Plus） |
| `frontend-admin/` | 平台管理后台（Vue 3 + Element Plus） |

配套工程与文档：

| 路径 | 说明 |
|---|---|
| `api_test_project/` | pytest + requests 接口自动化测试工程 |
| `deploy/` | Nginx 三站点 + systemd + 部署脚本 |
| `api.md` | 接口契约权威文档（113 个接口、错误码、种子账号） |
| `AGENTS.md` | 工程规范与 AI Agent 约束 |

> 设计取向：接口参数清晰、响应结构稳定、错误码分段规范，便于 Apifox 调试与 pytest 自动化断言。第三方服务未配置时自动降级，不阻塞本地开发。

---

## 核心功能

### 用户端

- 手机号注册 / 登录（JWT，Access Token 2 小时、Refresh Token 7 天）
- 商家列表、平台品类筛选、关键词搜索、商家详情与商品浏览
- 商品规格选择与购物车（Redis Hash，按商家分组）
- 收货地址管理（增删改查、设为默认）
- 下单：库存 Redis + Lua 原子预扣、`clientToken` 幂等、配送范围校验、营业时间校验
- 支付：支付宝沙箱 Precreate + 微信 Mock 通道（未配置时自动降级）
- 订单查询、取消、删除、售后退款（1 分钟内即时退 / 超时转商家审核）

### 商家端

- 手机号登录
- 店铺信息维护
- 商品、规格、菜单分类 CRUD
- 订单接单、拒单、制作、完成
- 退款申请审批
- WebSocket 实时推送新订单与退款申请

### 管理端

- 管理员登录
- 商家入驻审核（单条 / 批量通过、拒绝）
- 平台品类管理（CRUD、启停用、排序、批量操作）
- 用户管理（列表、详情、封禁 / 解封、删除、操作日志）
- 商家管理（列表、详情、封禁 / 解封、暂停 / 恢复营业、删除、操作日志）
- 订单管理（列表、详情、批量删除、单个 / 批量退款）
- 数据看板（核心指标、用户趋势、入驻趋势、订单状态分布、最近订单）

---

## 技术栈

### 后端

| 分类 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.3.5 |
| 安全 | Spring Security + JJWT（HS256） | 0.12.6 |
| ORM | MyBatis-Plus | 3.5.8 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis（Jedis 客户端） | 5.1.5 |
| 数据库迁移 | Flyway | 10.17.1 |
| 接口文档 | SpringDoc OpenAPI | 2.6.0 |
| 工具库 | Hutool / Lombok / Jackson | 5.8.34 |
| 图片存储 | 阿里云 OSS（服务端签名 + 前端直传） | - |
| 支付 | 支付宝沙箱 SDK + 微信 Mock | - |
| 地图 | 百度地图 Web API + Haversine 降级 | - |

### 前端

| 工程 | 技术栈 |
|---|---|
| `frontend-user` | Vue 3.4 + Vant 4 + Pinia + Vue Router + Axios + Vite 5 |
| `frontend-merchant` | Vue 3.4 + Element Plus + Pinia + Vue Router + Axios + Vite 5 |
| `frontend-admin` | Vue 3.4 + Element Plus + ECharts + Pinia + Vue Router + Axios + Vite 5 |

### 测试与部署

| 分类 | 技术 |
|---|---|
| 接口自动化 | pytest 8 + requests + PyYAML + Allure |
| 数据校验 | PyMySQL + redis-py |
| 辅助能力 | pytest-xdist、websocket-client、Faker |
| 部署 | Ubuntu 24.04 + Nginx + systemd + MySQL 8 + Redis 7 |

---

## 系统架构

```
                         浏览器
        ┌───────────────────┼───────────────────┐
      :80                :8081               :8082
   用户端 H5          商家后台            平台管理后台
 /var/www/nsp-user  /var/www/nsp-merchant  /var/www/nsp-admin
        └───────────────────┼───────────────────┘
                            │  /api/*  /ws/*  /v3/api-docs
                         Nginx 反向代理
                            │
                 127.0.0.1:8080  Spring Boot
                            │
            ┌───────────────┴───────────────┐
        MySQL 8（nsp_db）              Redis（db3）
```

- 后端默认端口 **8080**
- 三个前端工程均使用相对路径 `/api`，由 Vite（开发）或 Nginx（生产）同源代理
- 商家端 WebSocket 路径：`/ws/merchant?token=...`
- 生产环境建议将 MySQL / Redis 限制为仅本机或内网访问

---

## 目录结构

```
night-snack-platform/
├── backend/                        # Maven 多模块后端
│   ├── pom.xml                     # 父 POM，统一版本管理
│   ├── .env.example                # 环境变量模板
│   ├── run-dev.ps1                 # Windows 本地启动脚本
│   ├── .nsp-common/                # 公共模块：Result、异常、常量、JWT、OSS 工具
│   ├── .nsp-pojo/                  # 实体模块：entity / dto / vo
│   └── .nsp-server/                # 启动模块：controller / service / mapper / security / ws
│       └── src/main/resources/
│           ├── application.yml
│           ├── application-dev.yml.example
│           ├── application-prod.yml
│           ├── db/migration/V1__init.sql
│           └── lua/stock_deduct.lua
├── frontend-user/                  # 用户端 H5
├── frontend-merchant/              # 商家后台
├── frontend-admin/                 # 平台管理后台
├── api_test_project/               # pytest 接口自动化
│   ├── api/                        # 接口层
│   ├── common/                     # 配置加载、请求封装、断言、日志
│   ├── config/                     # dev / test / prod 环境配置
│   ├── data/                       # 测试数据
│   ├── testcases/                  # 测试用例
│   └── utils/                      # MySQL / Redis / 数据生成
├── deploy/                         # 部署脚本、Nginx、systemd
├── api.md                          # 接口契约权威文档
├── AGENTS.md                       # 工程规范
└── README.md
```

> **Controller 按角色分层**（`controller/user|merchant|admin`），**Service / Mapper 按业务域扁平放置**，可被不同角色的 Controller 复用。

---

## 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 后端必需 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 5.0+ | 缓存 / 库存 / 购物车 |
| Node.js | 18+ | 前端构建 |
| pnpm | 8+ | 前端包管理器 |
| Python | 3.10+ | 自动化测试（可选） |

### 1. 获取代码

```bash
git clone <仓库地址>
cd night-snack-platform
```

### 2. 创建数据库

```sql
CREATE DATABASE nsp_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> 表结构与种子数据由 Flyway 在首次启动时自动创建（`V1__init.sql`，共 16 张表，含 9 商家 / 60 商品 / 13 用户 / 28 订单演示数据）。

### 3. 配置后端

```bash
cd backend

# 1) 开发配置（仓库只跟踪 .example，需自行复制）
cp .nsp-server/src/main/resources/application-dev.yml.example \
   .nsp-server/src/main/resources/application-dev.yml

# 2) 环境变量（存放数据库口令、JWT 密钥、第三方密钥）
cp .env.example .env
```

Windows PowerShell：

```powershell
cd backend
Copy-Item .nsp-server/src/main/resources/application-dev.yml.example `
          .nsp-server/src/main/resources/application-dev.yml
Copy-Item .env.example .env
```

编辑 `backend/.env`，至少确认以下几项：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `NSP_DB_USER` | `root` | MySQL 用户名 |
| `NSP_DB_PASSWORD` | `123456` | MySQL 密码 |
| `NSP_REDIS_HOST` | `localhost` | Redis 地址 |
| `NSP_REDIS_PORT` | `6379` | Redis 端口 |
| `NSP_REDIS_PASSWORD` | `123456` | Redis 密码 |
| `NSP_REDIS_DATABASE` | `3` | **Redis 逻辑库，必须与后端一致** |
| `JWT_SECRET` | 开发默认值 | 生产环境必须替换为随机长字符串 |

> `.env` 已被 `.gitignore` 忽略，**禁止提交**。第三方服务密钥不填时对应功能自动降级，不影响启动。

### 4. 启动后端

Windows：

```powershell
cd backend
.\run-dev.ps1
```

Linux / macOS：

```bash
cd backend
set -a && source .env && set +a
mvn -pl .nsp-server spring-boot:run
```

或构建可执行 jar 后运行：

```bash
cd backend
mvn clean package -DskipTests
java -jar .nsp-server/target/nsp-server-1.0.0-SNAPSHOT.jar
```

启动成功后：

- 健康检查：<http://localhost:8080/actuator/health>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

### 5. 启动前端

三个工程需要分别启动（均为 pnpm）：

```bash
cd frontend-user
pnpm install
pnpm dev          # http://localhost:5175
```

```bash
cd frontend-merchant
pnpm install
pnpm dev          # http://localhost:5174
```

```bash
cd frontend-admin
pnpm install
pnpm dev          # http://localhost:5173
```

| 工程 | 开发端口 | 生产端口（Nginx） | 代理 |
|---|---|---|---|
| `frontend-user` | 5175 | 80 | `/api` → 8080 |
| `frontend-merchant` | 5174 | 8081 | `/api`、`/ws` → 8080 |
| `frontend-admin` | 5173 | 8082 | `/api` → 8080 |

生产构建：

```bash
pnpm build        # 产物输出到 dist/
```

### 6. 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 用户登录（注意：必须带 Content-Type: application/json）
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000003","password":"test123"}'
```

预期响应：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "userId": 1,
    "role": "USER",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

> ⚠️ 登录、下单等 JSON 接口**必须**携带 `Content-Type: application/json`。缺少该请求头会因消息转换失败返回 `code=1999`（系统繁忙），这是 JMeter 等工具最常见的踩坑点。

---

## 默认账号

以下账号由 Flyway 种子数据创建，**仅用于本地开发与测试环境**：

| 端 | 登录账号 | 密码 | 说明 |
|---|---|---|---|
| 平台管理后台 | `admin` | `admin123` | 用户名登录 |
| 商家后台 | `13800000002` | `m123456` | 手机号登录 |
| 用户端 H5 | `13800000003` | `test123` | 手机号登录 |

> 生产环境请务必修改默认密码，并通过环境变量注入数据库与 JWT 密钥。

---

## API 文档

| 文档 | 地址 | 说明 |
|---|---|---|
| 接口契约 | [`api.md`](api.md) | **权威文档**：113 个接口、请求/响应示例、错误码、状态机 |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | 匿名可访问 |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> | 可导入 Apifox / Postman |

### 统一响应格式

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 业务状态码，`0` 表示成功 |
| `msg` | string | 提示信息 |
| `data` | T | 业务数据，可能为 `null` |

> 业务失败时 **HTTP 状态码通常仍为 200**，请以 `code` 判断成功与否，不要只用 `status_code == 200` 断言。

### 鉴权与分页

- 鉴权：`Authorization: Bearer <accessToken>`
- 分页：`?page=1&size=20`，响应 `data` 包含 `total` 与 `list`
- 时间格式：ISO-8601 字符串（如 `2025-07-18T21:30:00`）

### 接口分组

| 角色 | URL 前缀 |
|---|---|
| 用户端 | `/api/user/**` |
| 商家端 | `/api/merchant/**` |
| 管理端 | `/api/admin/**` |
| 上传 | `/api/upload/**` |
| 支付回调 | `/api/pay/alipay/callback` |
| 令牌刷新 | `/api/auth/refresh` |

### 导入 Apifox

1. 启动后端
2. 打开 Apifox → **导入** → **URL 导入**
3. 填入 `http://localhost:8080/v3/api-docs`
4. 选择按 Tag 分组，自动生成全部接口

---

## 自动化测试

`api_test_project/` 是基于 pytest 的独立接口自动化工程，不参与 Maven 构建。

```bash
cd api_test_project

# 创建虚拟环境
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux / macOS
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env      # 填写数据库 / Redis 口令
pytest
```

### 常用命令

```bash
pytest                          # 全量
pytest -m smoke                 # 冒烟用例
pytest -m p1                    # P1 优先级
pytest -m "order and db"        # 组合标记
pytest -n 4                     # 多进程并行（pytest-xdist）

pytest --alluredir=reports/allure-results
allure serve reports/allure-results
```

### 用例标记

| 标记 | 含义 |
|---|---|
| `smoke` | 冒烟用例 |
| `p1` / `p2` / `p3` | 优先级 |
| `auth` | 登录与鉴权 |
| `browse` | 浏览 |
| `cart` | 购物车 |
| `order` | 订单 |
| `payment` | 支付 |
| `user` | 用户 / 地址 |
| `merchant` | 商家端 |
| `admin` | 管理后台 |
| `db` | 涉及数据库断言 |
| `redis` | 涉及 Redis 断言 |

### 工程结构

```
api_test_project/
├── api/            # 接口层（URL 只在这里出现）
├── common/         # 配置加载、requests 封装、断言、日志
├── config/         # dev / test / prod 环境 YAML
├── data/           # 测试数据
├── testcases/      # 用例
├── utils/          # MySQL / Redis / 数据生成
├── conftest.py     # 全局 fixture
└── pytest.ini      # pytest 配置与标记注册
```

> 环境通过 `NSP_ENV` 切换（`dev` / `test` / `prod`），敏感口令只放在 `.env`。

---

## 部署

完整的服务器部署步骤见 **[`deploy/README.md`](deploy/README.md)**。

```bash
# 1. 首次初始化（安装 JDK / MySQL / Redis / Nginx，创建目录与用户）
sudo bash deploy/server-setup.sh

# 2. 发布应用（后端 jar + 三个前端 dist + Nginx 配置 + systemd）
sudo bash deploy/remote-deploy.sh
```

部署后的访问入口：

| 入口 | 地址 |
|---|---|
| 用户端 H5 | `http://<服务器IP>/` |
| 商家后台 | `http://<服务器IP>:8081/` |
| 平台管理后台 | `http://<服务器IP>:8082/` |
| Swagger UI | `http://<服务器IP>/swagger-ui.html` |

> 后端由 systemd 托管（服务名 `nsp-server`），查看日志：`journalctl -u nsp-server -f`

---

## 配置说明

后端配置位于 `backend/.nsp-server/src/main/resources/`：

| 文件 | 说明 |
|---|---|
| `application.yml` | 公共配置（端口、Jackson、Flyway、MyBatis-Plus、Actuator、SpringDoc） |
| `application-dev.yml` | 开发环境配置（**不入库**，由 `.example` 复制） |
| `application-prod.yml` | 生产环境配置（全部走环境变量，无默认值） |

### 主要环境变量

| 变量 | 说明 |
|---|---|
| `NSP_DB_URL` / `NSP_DB_USER` / `NSP_DB_PASSWORD` | MySQL 连接 |
| `NSP_REDIS_HOST` / `NSP_REDIS_PORT` / `NSP_REDIS_PASSWORD` / `NSP_REDIS_DATABASE` | Redis 连接 |
| `JWT_SECRET` | JWT 签名密钥（生产必须替换） |
| `ALIYUN_OSS_ENDPOINT` / `ALIYUN_OSS_BUCKET` / `ALIYUN_OSS_AK` / `ALIYUN_OSS_SK` | 阿里云 OSS |
| `BAIDU_MAP_AK` | 百度地图 AK（不填则用 Haversine） |
| `ALIPAY_APP_ID` / `ALIPAY_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` / `ALIPAY_NOTIFY_URL` | 支付宝沙箱 |

各模板文件：

- `backend/.env.example`
- `deploy/.env.example`
- `api_test_project/.env.example`

> **Redis 逻辑库固定为 `db 3`**。直接执行 `redis-cli` 时默认连接 `db 0`，查询会看到空结果，请加 `-n 3`。

---

## 错误码规范

统一响应体中的 `code` 按业务域分段：

| 区间 | 业务域 |
|---|---|
| `0` | 成功 |
| `1000 - 1999` | 通用（鉴权、参数校验、系统） |
| `2000 - 2999` | 用户（注册、登录、地址、购物车、封禁） |
| `3000 - 3999` | 商品 / 商家 |
| `4000 - 4999` | 订单 |
| `5000 - 5999` | 支付 |
| `6000 - 6999` | 上传 |
| `7000 - 7999` | 商家管理 |
| `8000 - 8999` | 品类管理 |

常见错误码：

| 错误码 | 含义 |
|---|---|
| `1001` | 未登录 |
| `1002` | 无权限 |
| `1100` | 参数校验失败 |
| `1999` | 系统内部错误 |
| `2003` / `2004` | 用户不存在 / 密码错误 |
| `2303` | 账号已被封禁 |
| `3002` | 商家已打烊 |
| `3104` | 库存不足 |
| `4002` | 订单状态不允许此操作 |
| `4003` | 超出配送范围 |
| `7003` | 商家已被封禁 |

完整错误码见 [`api.md`](api.md) 第二节。

---

## 安全说明

- **所有密钥走环境变量**：数据库口令、JWT 密钥、OSS AK/SK、百度 AK、支付宝密钥均不写入代码或配置模板
- `.env`、`application-dev.yml`、`application-prod.yml` 中的真实值均被 `.gitignore` 忽略
- 仓库内只保留 `.env.example` 等模板文件，值全部为 `placeholder`
- 密码使用 BCrypt 加密存储，不保存明文
- 用户 / 商家 / 管理员三级鉴权，接口按角色前缀隔离
- 订单、地址等资源查询强制带当前用户 / 商家 ID，防止水平越权
- 支付回调校验签名并做幂等处理（`payment_record` 唯一约束）
- 库存扣减使用 Redis Lua 脚本保证原子性，禁止 Java 侧先读后写
- 生产环境应限制 MySQL / Redis 端口仅本机或内网访问，推荐通过 SSH 隧道连接

> ⚠️ 发现密钥泄漏时，请立即在对应平台轮换密钥，并清理 Git 历史，而不是仅仅删除文件。

---

## 常见问题

**Q：登录 / 下单接口返回 `code=1999`「系统繁忙，请稍后再试」？**

A：检查请求头是否缺少 `Content-Type: application/json`。JMeter 的 Body Data 默认以 `text/plain` 发送，必须添加 HTTP 信息头管理器。

**Q：Redis 里查不到购物车 / 库存数据？**

A：后端使用 `db 3`。`redis-cli` 默认连接 `db 0`，请加 `-n 3`。

**Q：图片上传接口报错？**

A：未配置阿里云 OSS AK/SK 时上传功能不可用，但商品图使用外链种子数据，浏览与下单不受影响。

**Q：支付宝支付不可用？**

A：未配置支付宝沙箱参数时自动降级；微信支付使用 `WECHAT_MOCK` 通道可直接走通。

**Q：前端请求 401？**

A：Access Token 默认 2 小时过期，请通过 `/api/auth/refresh` 刷新或重新登录。

**Q：服务启动报数据库连接失败？**

A：确认 MySQL 已启动、`nsp_db` 已创建、`backend/.env` 中的账号密码正确。

---

## 相关文档

| 文档 | 说明 |
|---|---|
| [`api.md`](api.md) | 接口契约（权威） |
| [`AGENTS.md`](AGENTS.md) | 工程规范、目录约定、安全红线 |
| [`deploy/README.md`](deploy/README.md) | 服务器部署说明 |
| [`backend/.env.example`](backend/.env.example) | 后端环境变量模板 |
| [`api_test_project/.env.example`](api_test_project/.env.example) | 测试工程环境变量模板 |

---

## 许可证

本仓库当前未包含开源许可证文件，仅供学习与技术交流使用；未经作者授权，请勿用于商业用途。
