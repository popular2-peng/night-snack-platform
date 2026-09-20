#!/usr/bin/env bash
# ============================================================
# 夜宵外卖平台 — 服务器基础环境初始化
#
# 适用：Ubuntu 24.04（全新机器）
# 用法：sudo bash server-setup.sh
#
# 做什么：
#   1. 时区设为 Asia/Shanghai（营业时间判断依赖，必须）
#   2. 安装 JDK 17 / MySQL 8 / Redis / Nginx
#   3. 创建运行用户 nsp 与目录
#   4. 初始化 MySQL（建库 nsp_db + 专用账号）与 Redis（密码 + 仅本机监听）
#   5. 生成 /opt/nsp/.env（含随机密码，权限 600）
#   6. 针对 2C/4G 小内存做轻量调优
#
# 特性：幂等。重复执行不会重置已生成的密码（会复用 .env 中的值）
#
# ⚠️ 本脚本不会输出任何密码明文；密码只写入 /opt/nsp/.env
# ============================================================
set -euo pipefail

NSP_HOME=/opt/nsp
NSP_USER=nsp
NSP_DB_NAME=nsp_db
NSP_DB_USER=nsp
ENV_FILE="$NSP_HOME/.env"
WWW_ROOT=/var/www

log() { echo -e "\n\033[1;36m[setup]\033[0m $*"; }
ok()  { echo -e "  \033[1;32m✓\033[0m $*"; }

[[ $EUID -eq 0 ]] || { echo "请以 root 运行：sudo bash $0"; exit 1; }

# ------------------------------------------------------------
# 1. 时区
# ------------------------------------------------------------
log "设置时区为 Asia/Shanghai"
timedatectl set-timezone Asia/Shanghai
ok "$(date '+%Y-%m-%d %H:%M:%S %Z')"

# ------------------------------------------------------------
# 2. 安装依赖包
# ------------------------------------------------------------
log "安装软件包（JDK 17 / MySQL 8 / Redis / Nginx）"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq \
    openjdk-17-jre-headless \
    mysql-server \
    redis-server \
    nginx \
    openssl curl >/dev/null
ok "java: $(java -version 2>&1 | head -1)"
ok "mysql: $(mysql --version | awk '{print $3}')"
ok "redis: $(redis-server --version | awk '{print $3}')"
ok "nginx: $(nginx -v 2>&1 | cut -d/ -f2)"

# ------------------------------------------------------------
# 3. 运行用户与目录
# ------------------------------------------------------------
log "创建运行用户与目录"
if ! id -u "$NSP_USER" >/dev/null 2>&1; then
    useradd --system --no-create-home --shell /usr/sbin/nologin "$NSP_USER"
    ok "已创建系统用户 $NSP_USER（不可登录）"
else
    ok "用户 $NSP_USER 已存在"
fi
mkdir -p "$NSP_HOME" "$WWW_ROOT/nsp-user" "$WWW_ROOT/nsp-merchant" "$WWW_ROOT/nsp-admin"
mkdir -p /var/log/nsp
chown -R "$NSP_USER:$NSP_USER" "$NSP_HOME" /var/log/nsp
ok "$NSP_HOME / $WWW_ROOT/nsp-* 目录就绪"

# ------------------------------------------------------------
# 4. 小内存调优（2 vCPU / 3.8 GB）
# ------------------------------------------------------------
log "应用小内存 MySQL 配置"
cat > /etc/mysql/mysql.conf.d/zz-nsp.cnf <<'MYSQLCNF'
# 夜宵外卖平台 — 小内存虚机调优（2C/4G）
[mysqld]
innodb_buffer_pool_size       = 256M
performance_schema            = OFF
max_connections               = 100
innodb_flush_log_at_trx_commit = 2
character-set-server          = utf8mb4
collation-server              = utf8mb4_unicode_ci
default-time-zone             = '+08:00'
MYSQLCNF
# ⚠️ 必须重启：apt 安装时 MySQL 已被启动，此后写入的配置不会自动加载
systemctl restart mysql
ok "innodb_buffer_pool=256M, performance_schema=OFF, TZ=+08:00"

# ------------------------------------------------------------
# 5. 生成/复用密钥
# ------------------------------------------------------------
log "准备密钥"
if [[ -f "$ENV_FILE" ]]; then
    # 复用已有密码，避免重启后连不上库
    DB_PASS=$(grep '^NSP_DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
    REDIS_PASS=$(grep '^NSP_REDIS_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
    JWT_SECRET=$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2-)
    ok "复用已有 $ENV_FILE 中的密钥"
else
    DB_PASS=$(openssl rand -hex 16)
    REDIS_PASS=$(openssl rand -hex 16)
    JWT_SECRET=$(openssl rand -hex 32)
    ok "已生成新密钥（仅写入 $ENV_FILE）"
fi

# ------------------------------------------------------------
# 6. MySQL 初始化
# ------------------------------------------------------------
log "初始化 MySQL（库 $NSP_DB_NAME / 账号 $NSP_DB_USER）"
systemctl enable --now mysql >/dev/null 2>&1 || true
# Ubuntu 的 MySQL root 默认走 auth_socket，root 直接执行即可
mysql --protocol=socket -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`$NSP_DB_NAME\`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$NSP_DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
ALTER  USER '$NSP_DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON \`$NSP_DB_NAME\`.* TO '$NSP_DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL
ok "库与账号就绪（建表交给应用启动时的 Flyway 迁移）"

# ------------------------------------------------------------
# 7. Redis 初始化
# ------------------------------------------------------------
log "初始化 Redis（要求密码 + 仅监听本机）"
REDIS_CONF=/etc/redis/redis.conf
sed -i "s/^# *requirepass .*/requirepass $REDIS_PASS/" "$REDIS_CONF"
grep -q '^requirepass ' "$REDIS_CONF" || echo "requirepass $REDIS_PASS" >> "$REDIS_CONF"
sed -i 's/^# *maxmemory .*/maxmemory 256mb/' "$REDIS_CONF"
grep -q '^maxmemory ' "$REDIS_CONF" || echo "maxmemory 256mb" >> "$REDIS_CONF"
sed -i 's/^# *maxmemory-policy .*/maxmemory-policy allkeys-lru/' "$REDIS_CONF"
grep -q '^maxmemory-policy ' "$REDIS_CONF" || echo "maxmemory-policy allkeys-lru" >> "$REDIS_CONF"
sed -i 's/^bind .*/bind 127.0.0.1 -::1/' "$REDIS_CONF"
systemctl enable --now redis-server >/dev/null 2>&1 || true
systemctl restart redis-server
ok "Redis 已重启并生效"

# ------------------------------------------------------------
# 8. 生成 /opt/nsp/.env
# ------------------------------------------------------------
log "写入 $ENV_FILE"
cat > "$ENV_FILE" <<ENVEOF
# ============================================================
# 夜宵外卖平台 — 服务器运行环境变量（由 server-setup.sh 生成）
# ⚠️ 含真实密码，权限 600，禁止提交进 git
# 修改后需 systemctl restart nsp-server 生效
# ============================================================

# ---------- 数据库 ----------
NSP_DB_URL=jdbc:mysql://127.0.0.1:3306/${NSP_DB_NAME}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
NSP_DB_USER=${NSP_DB_USER}
NSP_DB_PASSWORD=${DB_PASS}

# ---------- Redis ----------
NSP_REDIS_HOST=127.0.0.1
NSP_REDIS_PORT=6379
NSP_REDIS_PASSWORD=${REDIS_PASS}
NSP_REDIS_DATABASE=3

# ---------- JWT（本服务器独有随机密钥）----------
JWT_SECRET=${JWT_SECRET}

# ---------- 阿里云 OSS（未接入，占位值 -> 上传功能走降级）----------
# 注意：endpoint 必须是合法域名格式，否则启动时 OSS 客户端构造失败
ALIYUN_OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
ALIYUN_OSS_BUCKET=placeholder
ALIYUN_OSS_AK=placeholder
ALIYUN_OSS_SK=placeholder
ALIYUN_OSS_REGION=cn-beijing
ALIYUN_OSS_CDN=placeholder
ALIYUN_OSS_CALLBACK=placeholder

# ---------- 百度地图（未接入，占位值 -> 自动降级 Haversine）----------
BAIDU_MAP_AK=placeholder

# ---------- 支付宝沙箱（未接入，占位值 -> 支付走 mock 降级）----------
ALIPAY_APP_ID=placeholder
ALIPAY_PRIVATE_KEY=placeholder
ALIPAY_PUBLIC_KEY=placeholder
ALIPAY_NOTIFY_URL=placeholder
ENVEOF
chown "$NSP_USER:$NSP_USER" "$ENV_FILE"
chmod 600 "$ENV_FILE"
ok "$ENV_FILE 已写入（权限 600，属主 $NSP_USER）"

# ------------------------------------------------------------
# 9. 完成
# ------------------------------------------------------------
log "基础环境就绪，汇总"
echo "  时区     : $(timedatectl show -p Timezone --value)"
echo "  Java     : $(java -version 2>&1 | head -1)"
echo "  MySQL    : $(systemctl is-active mysql)"
echo "  Redis    : $(systemctl is-active redis-server)  ($(redis-cli -a "$REDIS_PASS" --no-auth-warning ping 2>/dev/null))"
echo "  Nginx    : $(systemctl is-active nginx)"
echo "  服务用户 : $NSP_USER"
echo "  应用目录 : $NSP_HOME"
echo
echo "下一步：在本地执行 deploy/deploy.sh 上传后端 jar 与前端产物"
