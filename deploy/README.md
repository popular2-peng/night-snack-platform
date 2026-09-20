# 部署说明（deploy/）

把夜宵外卖平台全栈部署到一台 Ubuntu 服务器：三个前端 + Spring Boot 后端 + MySQL + Redis。

## 部署架构

```
                     浏览器
        ┌───────────────┼───────────────┐
      :80             :8081           :8082
   用户端 H5        商家后台         管理后台
  /var/www/nsp-user  /var/www/nsp-merchant  /var/www/nsp-admin
        └───────────────┼───────────────┘
                        │  /api/*  /ws/*  /v3/api-docs  (Nginx 反代)
                        ▼
             127.0.0.1:8080  Spring Boot (systemd: nsp-server)
                        ├── 127.0.0.1:3306  MySQL 8  → nsp_db
                        └── 127.0.0.1:6379  Redis   → db3
```

- 后端**只监听 127.0.0.1:8080**，外部一律经 Nginx 访问，不直接暴露
- MySQL / Redis 同样只监听本机
- 三个前端按**端口**分站点，因为它们的 vite 构建产物资源路径是绝对的 `/assets/...`，
  且 axios `baseURL` 是相对路径 `/api`（见各工程 `src/apis/request.ts`），
  所以走同源反代即可，**无需改代码或重新构建 base 路径**

## 目录内容

| 文件 | 说明 |
|---|---|
| `server-setup.sh` | 服务器基础环境初始化：时区、装包、建用户、初始化 MySQL/Redis、生成 `.env` |
| `remote-deploy.sh` | 应用部署：安装 jar / 前端产物 / Nginx 站点 / systemd 服务并启动 |
| `nginx/nsp-*.conf` | 三个站点的 Nginx 配置 |
| `systemd/nsp-server.service` | 后端 systemd 服务单元 |
| `.env.example` | 服务器端环境变量清单（仅占位符，供排查参考） |

> 真实的 `/opt/nsp/.env` 由 `server-setup.sh` 生成（含随机密码，权限 600），**不进 git**。

## 首次部署

### 1. 服务器基础环境（只需一次）

```bash
scp deploy/server-setup.sh <user>@<host>:/tmp/
ssh <user>@<host>
sudo bash /tmp/nsp-server-setup.sh
```

脚本会：设时区 `Asia/Shanghai` → 装 JDK17/MySQL8/Redis/Nginx → 建系统用户 `nsp` 与 `/opt/nsp`、
`/var/www/nsp-*` → 建库 `nsp_db` 与账号 `nsp@localhost`（随机密码）→ 配 Redis 密码与仅本机监听 →
写 `/opt/nsp/.env` → 小内存调优。

> ⚠️ 时区必须设置：商家营业时间跨夜（如 18:00–03:00），时区错误会让「是否营业中」判断偏移 8 小时。

### 2. 构建产物（本地）

```bash
# 后端：必须产出可执行 fat jar
cd backend && mvn clean package -DskipTests
ls -lh .nsp-server/target/*.jar      # 约 95~99 MB；若只有几百 KB 说明打成了瘦 jar

# 前端：三个工程各自构建
cd frontend-user     && pnpm build
cd frontend-merchant && pnpm build
cd frontend-admin    && pnpm build
```

> 后端 `pom.xml` 里 `spring-boot-maven-plugin` 显式绑定了 `repackage` 目标。
> 本工程 parent 是自定义的（不是 `spring-boot-starter-parent`），不绑定就只会产出不可执行的瘦 jar。

### 3. 上传产物

暂存目录要以**登录用户**身份创建（不是 root），否则 scp/sftp 会因权限被拒：

```bash
ssh <user>@<host> 'rm -rf /tmp/nsp-artifacts && mkdir -p /tmp/nsp-artifacts/{nginx,systemd,www}'

scp backend/.nsp-server/target/nsp-server-1.0.0-SNAPSHOT.jar <user>@<host>:/tmp/nsp-artifacts/nsp-server.jar
scp deploy/remote-deploy.sh                                 <user>@<host>:/tmp/nsp-artifacts/
scp deploy/nginx/*.conf                                     <user>@<host>:/tmp/nsp-artifacts/nginx/
scp deploy/systemd/nsp-server.service                       <user>@<host>:/tmp/nsp-artifacts/systemd/
scp -r frontend-user/dist/.     <user>@<host>:/tmp/nsp-artifacts/www/nsp-user/
scp -r frontend-merchant/dist/. <user>@<host>:/tmp/nsp-artifacts/www/nsp-merchant/
scp -r frontend-admin/dist/.    <user>@<host>:/tmp/nsp-artifacts/www/nsp-admin/
```

### 4. 部署

```bash
ssh <user>@<host> 'sudo bash /tmp/nsp-artifacts/remote-deploy.sh'
```

脚本幂等，会安装产物、启用 Nginx 站点、重启 systemd 服务，并轮询 `/actuator/health` 直到就绪。

## 重新发版

重复上面第 2～4 步即可（`remote-deploy.sh` 幂等）。只改后端时，可跳过前端上传。

## 验收

```bash
HOST=<服务器IP>

# 后端存活
curl -s http://$HOST/actuator/health                      # {"status":"UP"}

# OpenAPI 描述（Apifox「导入 → OpenAPI」填这个地址）
curl -s http://$HOST/v3/api-docs | head -c 200

# 三个前端
for p in 80 8081 8082; do curl -s -o /dev/null -w "$p -> %{http_code}\n" http://$HOST:$p/; done

# 三角色登录（种子数据）
curl -s -X POST http://$HOST/api/admin/login    -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}'
curl -s -X POST http://$HOST/api/merchant/login -H 'Content-Type: application/json' -d '{"phone":"13800000002","password":"m123456"}'
curl -s -X POST http://$HOST/api/user/login     -H 'Content-Type: application/json' -d '{"phone":"13800000003","password":"test123"}'
```

访问地址：

| 入口 | 地址 |
|---|---|
| 用户端 H5 | `http://<host>/` |
| 商家后台 | `http://<host>:8081/` |
| 管理后台 | `http://<host>:8082/` |
| Swagger UI | `http://<host>/swagger-ui.html` |

## 运维命令

```bash
systemctl status nsp-server          # 服务状态
journalctl -u nsp-server -f          # 实时日志
systemctl restart nsp-server         # 重启后端
sudo cat /opt/nsp/.env               # 查看生成的密码（600 权限）
```

## 常见问题

**登录/下单报错、服务起不来** → 先看日志：`journalctl -u nsp-server -n 100 --no-pager`

**后端启动即退出** → 多半是 `/opt/nsp/.env` 缺变量。`application-prod.yml` 里所有 `${}` 都没有默认值，
缺一个就启动失败。完整清单见 `.env.example`。特别注意：
- `NSP_DB_URL`（`backend/.env.example` 里没有这一项，最容易漏）
- `JWT_SECRET` 必须 ≥ 32 字符，否则 `JwtUtil` 构造器直接抛异常
- `ALIYUN_OSS_ENDPOINT` 不能是空串（会触发 URI 解析异常），填合法域名或 `placeholder` 都行

**上传图片接口报错** → 正常。OSS 未接入（`placeholder`），`/api/upload/sign` 会在调用时失败，
但不影响进程与其他功能。

**超时自动退款日志刷 ERROR** → 正常。支付宝未接入（`placeholder`），
超时订单的自动退款会失败并每分钟重试一次（`OrderTimeoutScheduler`）。

**配送距离不准** → 正常。百度地图 AK 未接入时自动降级为 Haversine 直线距离。

## 安全说明

- 服务器 `/opt/nsp/.env` 含随机密码，权限 `600`、属主 `nsp`，**不要提交进 git**
- 服务器上未存放任何真实第三方密钥（OSS / 百度 / 支付宝全为 `placeholder`）
- MySQL root 保持 `auth_socket`（只能本机 sudo 登录），应用使用独立的 `nsp@localhost` 账号
- 未开启 ufw，避免误锁 SSH。如需加固，先放行 22 端口再启用：
  `sudo ufw allow 22/tcp && sudo ufw allow 80,8081,8082/tcp && sudo ufw enable`
- 已知问题：`UploadController.callback()` 注释写了「白名单 IP 校验」，但代码中**未实现**该校验，
  是匿名可写的 POST。当前内网测试环境可接受，对外暴露前需补上验签或 IP 限制。
