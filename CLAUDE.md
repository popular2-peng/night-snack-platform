# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 夜宵外卖平台（Night Snack Platform / `nsp`）—— Spring Boot 3 后端 + 三个 Vue3 前端 + 一套 pytest 接口自动化。
> 本文件是**给 Agent 用的操作手册**：命令、架构、约定、红线。
> 项目背景与完整规范见 `AGENTS.md`；**接口契约以 `api.md` 为准**。

---

## 一、常用命令

### 后端（Java 17 + Spring Boot 3.3.5，Maven 多模块）

```bash
cd backend                              # ⚠️ 父 pom 在 backend/ 下，仓库根目录没有 pom.xml

mvn install -DskipTests                 # 编译打包（改完代码先跑，确认能编译）
mvn spring-boot:run -pl .nsp-server     # 启动（dev profile，端口 8080）

./run-dev.ps1                           # Windows 更省事：脚本会自动加载 backend/.env
```

启动成功的标志：日志出现 `Started NsPApplication in x.x seconds`。
健康检查：`GET http://localhost:8080/actuator/health`。

### 前端（Vite + Vue3 + TypeScript，三个独立工程）

```bash
cd frontend-user        # 或 frontend-merchant / frontend-admin
npm install
npm run dev             # 开发服务器
npm run build           # 构建（含 vue-tsc 类型检查，**必须过**）
```

### 接口自动化（pytest，独立工程，不在 Maven 里）

```bash
cd api_test_project
python -m common.data_loader      # 校验 data/*.yaml 语法（改完数据先跑这个）
pytest -vs                        # 跑全部（-vs 已写进 pytest.ini 的 addopts）
pytest -vs testcases/test_order.py
pytest -vs -m smoke               # 只跑冒烟
```

### 部署

```bash
bash deploy/server-setup.sh       # 服务器初始化（Ubuntu，一次性）
bash deploy/remote-deploy.sh      # 发布：停服 → 备份 → 替换 → 重启 → 健康检查
```

详见 `deploy/README.md`。

---

## 二、架构

### 后端三个 Maven 模块

- **`.nsp-common`** —— 通用层：`Result` 统一响应、`BizException`、`ErrorCode` / `MessageConst` 常量、JWT 工具、OSS 工具
- **`.nsp-pojo`** —— 数据模型：`entity` / `dto` / `vo`
- **`.nsp-server`** —— 业务与启动：`controller` / `service` / `mapper` / `security` / `aspect` / `config` / `ws`

**分层规则（最容易搞混，务必分清）**：

- `controller` **按调用角色分目录**：`controller/user`、`controller/merchant`、`controller/admin`
- `service` / `mapper` **扁平、按业务域命名**，不按角色分 —— 同一个 `OrderService` 被三端复用
- 角色边界靠 `@RequireRole("MERCHANT")` 注解 + `RoleAspect` 切面拦截

### 请求链路

```
请求
  → JwtAuthenticationFilter   解析 Bearer token，写入 SecurityContext
  → RoleAspect                校验 @RequireRole
  → Controller                @Valid 参数校验
  → Service                   业务规则 + 事务
  → Mapper                    MyBatis-Plus
  → Result{code,msg,data}
```

异常统一由 `@ControllerAdvice` 捕获并转成 `Result`，**不要把堆栈抛给前端**。

### 数据存储的职责划分

**MySQL** —— 业务数据：`user` / `merchant` / `product` / `product_spec` / `order` / `order_item` /
`order_status_log` / `payment_record` / `uploaded_file` 等（Flyway 管理建表，种子 SQL 只有 `V1__init.sql`）。

**Redis** —— 热数据与并发控制：

- `cart:{user_id}` —— 购物车（Hash）
- `product:stock:{sku_id}` —— 库存（⚠️ **key 用 specId，不是 productId**）
- `order:client-token:{userId}:{token}` —— 下单幂等键（TTL 10 分钟）
- `order:no:{yyyyMMdd}` —— 订单号自增序号（按天分桶）
- `upload:sign:limit:{user_id}` —— 上传签名限流

### 三端前端

`frontend-user`（H5 / Vant）、`frontend-merchant`（商家后台 / Element Plus）、
`frontend-admin`（平台后台 / Element Plus）。
三者都用 Vite 代理把 `/api`、`/ws` 转发到后端 8080。---

## 三、代码约定

- **统一响应**：所有接口返回 `Result<T>{ code, msg, data }`，`code=0` 表示业务成功；业务码不是 HTTP 状态码
- **错误码按域分段**（新增错误码必须放进对应段，不要乱塞）：

  - `1xxx` 通用 / `2xxx` 用户 / `3xxx` 商品商家 / `4xxx` 订单 / `5xxx` 支付 / `6xxx` 上传 / `7xxx` 商家管理 / `8xxx` 品类
  - 封禁码按**域**归属：用户与管理员被封禁 → `2303`；商家被封禁 → `7003`；`1002` 只表示「角色无权限」

- **DTO / VO 分离**：入参用 DTO，出参用 VO，**禁止把 Entity 直接返回前端**
- **金额一律 `BigDecimal`**，比较用 `compareTo`（`equals` 会因为 4.50 与 4.5 的标度不同而返回 false）
- **密码 BCrypt**，禁止明文 / MD5
- **日志**：SLF4J，下单、支付回调、退款、上传等关键链路必须带 `orderNo` / `userId` / `ossKey`
- **不要过度设计**：MVP 阶段不引 MQ、不分库分表、不做读写分离、不加无意义的重度函数式写法

---

## 四、业务规则（写 Service 时必须遵守）

**订单状态机** —— 只允许合法迁移，非法迁移抛 `4002`：

```
PENDING_PAY → PAID → ACCEPTED → COOKING → COMPLETED
     ↓           ↓         ↓         ↓
  CANCELLED   REFUNDING ←───────────┘
                  ↓
              REFUNDED
```

**库存** —— 必须用 **Redis Lua 脚本原子扣减**，禁止「Java 先读再写」；订单取消 / 超时要释放预扣库存。

**下单幂等** —— `setIfAbsent` 抢幂等键必须在**扣库存之前**，否则并发重复请求会二次扣库存。

**配送范围** —— 服务端按商家坐标 + 用户地址坐标算距离，**前端传的坐标只作参考**；
百度地图不可用时降级为本地 Haversine 计算。

**营业时间** —— 下单时校验商家 `status=OPEN` 且当前时间在营业时间段内，否则 `4004`（非营业时间）/ `3002`（商家已打烊）。

**退款两条分支**（分界点 60 秒）：

- 支付时间距今 `< 60s` → 即时退款，直接 `REFUNDED`
- 支付时间距今 `>= 60s` → 置 `REFUNDING`，通过 WebSocket 推 `REFUND_REQUEST` 给商家
  商家同意 → `REFUNDED`；拒绝 → **回滚到申请前的状态**（`PAID`/`ACCEPTED`/`COOKING`）

**超时任务**（`OrderTimeoutScheduler`，每 60 秒扫一次，操作人记 `system`）：
未支付 15 分钟 → 取消；支付后 15 分钟商家未接单 → 取消 + 自动退款；制作中超过 30 分钟 → 自动完成。

**购物车 cartKey 约定** —— 有规格 = `{specId}`；无规格 = `p{productId}`。
购物车按商家分组返回（`CartVO.groups`），禁止跨店合并下单。

**订单号** —— `NSP + yyyyMMdd + 6 位自增`，序号由 Redis 原子自增，Redis 不可用时降级为时间戳 + 随机数。

---

## 五、安全红线（最高优先级）

- **所有密钥只走环境变量**：`ALIYUN_OSS_AK/SK`、`BAIDU_MAP_AK`、`ALIPAY_*`、`JWT_SECRET`、数据库口令 —— 一个都不许硬编码进代码或 yml
- **`.env` 必须被忽略**：仓库根目录、`backend/`、`api_test_project/` 三处都要有对应规则；`.env.example` 里只能放 `placeholder`
- **`backup/` 与 `*.sql` 已忽略**（数据库导出含真实业务数据），仅 `backend/.nsp-server/src/main/resources/db/migration/*.sql` 例外
- **提交前自检**：搜一遍有没有 `LTAI` 开头、Base64 长串、`password: 明文` 之类的东西 —— 发现即撤回
- **支付回调必须验签**，同一 `out_trade_no` 只处理一次（幂等）
- **OSS Bucket 私有**，上传走服务端签名 + 前端直传，回调必须验签

---

## 六、测试约定（`api_test_project/`）

**三层结构，职责不要混**：

- `api/` —— 接口层：URL + method + 参数拼装。**`/api/xxx` 字符串只允许出现在这里**
- `data/*.yaml` —— 测试数据：业务输入与期望值（跨环境不变）
- `testcases/` —— 用例：调 api 方法 + 断言，**不写 URL、不写死 id**

**其他约定**：

- 环境配置在 `config/{dev,test,prod}.yaml`，敏感值用 `${VAR}` 由 `.env` 注入
- 每次运行必须不同的数据（手机号、`clientToken`）用 `utils/data_generator.py` 生成，**不要写进 YAML**
- 断言要下钻到数据库 / Redis（`utils/da_helper.py`、`utils/redis_helper.py`）：
  **接口返回 200 不等于数据对** —— 订单没落库、库存没扣，接口照样可能返回成功
- 新增接口后，同步把方法加进 `api/` 层对应的类里

---

## 七、文档地图

- **`api.md`** —— 接口契约的**唯一权威源**（112 个 REST 端点 + WS 协议 + 错误码表 + 种子账号），改接口必须同步它
- **`README.md`** —— 快速开始、目录结构、环境准备
- **`AGENTS.md`** —— 项目定位、技术栈、数据库设计、OSS 规范、Agent 边界
- **`deploy/README.md`** —— 部署架构与脚本用法
- Swagger UI：`http://localhost:8080/swagger-ui.html`（OpenAPI JSON：`/v3/api-docs`）

---

## 八、已知的坑（踩过，别再踩）

1. **商家端接口基本不传 `merchantId`** —— 后端从 JWT 的 `sub` 解析当前商家；
   **唯一例外**是 `GET /api/merchant/product/list`，它必须显式传
2. **`merchantId` 的位置在两个接口里不一样** —— 加购是 **query 参数**（`POST /api/user/cart?merchantId=1`），
   下单是 **body 字段**
3. **库存 key 用 `specId`** 不是 `productId`（`product:stock:{specId}`）；且该 key 是**懒加载**的，
   第一次下单才从 `product_spec.stock` 载入
4. **无规格商品全库只有一个**（`product.id = 61`）：加购/下单时**不要传 `specId`**，cartKey 为 `p61`；
   有规格商品不传 `specId` 会被拦 `3106`
5. **深夜食堂营业时间 18:00–03:00** —— 白天跑「商家列表」类用例可能看不到它，
   列表断言别依赖具体商家（用商家详情接口按 id 查）
6. **支付沙箱未配置会自动降级成 mock** —— `ALIPAY_APP_ID=placeholder` 时 `qrCode` 返回 null 但支付成功
7. **`payment_record` 有两个状态列** —— `status`（PENDING/SUCCESS/FAILED/REFUNDED）和
   `refund_status`（REFUNDING/REFUNDED）；退款**中**时 `status` 仍是 `SUCCESS`，别断言错列
8. **改店铺/商品状态必须恢复** —— 把店铺改成 `CLOSED` 会让所有下单用例挂掉

---

请认真工作，codex在看着你