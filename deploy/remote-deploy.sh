#!/usr/bin/env bash
# ============================================================
# 夜宵外卖平台 — 应用部署（在服务器上执行，需 root）
#
# 前置：产物已上传到 /tmp/nsp-artifacts/，目录结构：
#   /tmp/nsp-artifacts/
#   ├── nsp-server.jar              # Spring Boot fat jar
#   ├── remote-deploy.sh            # 本脚本
#   ├── nginx/nsp-{user,merchant,admin}.conf
#   ├── systemd/nsp-server.service
#   └── www/nsp-{user,merchant,admin}/   # 三个前端 dist 内容
#
# 用法：sudo bash /tmp/nsp-artifacts/remote-deploy.sh
#
# 特性：幂等 —— 可反复执行用于发版
# ============================================================
set -euo pipefail

ART=/tmp/nsp-artifacts
NSP_HOME=/opt/nsp
NSP_USER=nsp

log() { echo -e "\n\033[1;36m[deploy]\033[0m $*"; }
ok()  { echo -e "  \033[1;32m✓\033[0m $*"; }

[[ $EUID -eq 0 ]] || { echo "请以 root 运行：sudo bash $0"; exit 1; }
[[ -d "$ART" ]] || { echo "找不到 $ART，请先上传产物"; exit 1; }

# ------------------------------------------------------------
# 1. 后端 jar
# ------------------------------------------------------------
log "部署后端 jar"
[[ -f "$ART/nsp-server.jar" ]] || { echo "缺少 $ART/nsp-server.jar"; exit 1; }
install -o "$NSP_USER" -g "$NSP_USER" -m 644 "$ART/nsp-server.jar" "$NSP_HOME/nsp-server.jar"
ok "$(du -h "$NSP_HOME/nsp-server.jar" | cut -f1) → $NSP_HOME/nsp-server.jar"

# 校验确实是可执行 fat jar（防止误传瘦 jar）
# 注意：Ubuntu 最小安装没有 unzip，这里用系统自带的 python3 校验
if ! python3 -c "
import sys, zipfile
try:
    names = zipfile.ZipFile(sys.argv[1]).namelist()
except Exception:
    sys.exit(1)
sys.exit(0 if any(n.startswith('BOOT-INF/') for n in names) else 1)
" "$NSP_HOME/nsp-server.jar"; then
    echo "  ✗ 该 jar 不含 BOOT-INF/，不是 Spring Boot 可执行 jar！"
    echo "    请在本地执行 mvn clean package 后重新上传。"
    exit 1
fi
ok "已校验为可执行 fat jar"

# ------------------------------------------------------------
# 2. 前端静态产物
# ------------------------------------------------------------
log "部署三个前端"
for site in user merchant admin; do
    src="$ART/www/nsp-$site"
    dst="/var/www/nsp-$site"
    if [[ -d "$src" ]]; then
        # 先清空旧产物，避免残留的旧 hash 文件
        rm -rf "${dst:?}"/*
        cp -r "$src"/. "$dst"/
        chown -R www-data:www-data "$dst"
        ok "nsp-$site: $(find "$dst" -type f | wc -l) 个文件"
    else
        echo "  ⚠️  跳过 nsp-$site（$src 不存在）"
    fi
done

# ------------------------------------------------------------
# 3. Nginx 站点配置
# ------------------------------------------------------------
log "配置 Nginx 站点"
for site in user merchant admin; do
    conf="$ART/nginx/nsp-$site.conf"
    [[ -f "$conf" ]] || { echo "  ⚠️  缺少 $conf"; continue; }
    install -m 644 "$conf" "/etc/nginx/sites-available/nsp-$site"
    ln -sf "/etc/nginx/sites-available/nsp-$site" "/etc/nginx/sites-enabled/nsp-$site"
    ok "nsp-$site 站点已启用"
done
# 移除 Ubuntu 自带默认站点，避免与 :80 default_server 冲突
rm -f /etc/nginx/sites-enabled/default
ok "已移除默认站点"

nginx -t
systemctl reload nginx
ok "Nginx 配置校验通过并已重载"

# ------------------------------------------------------------
# 4. systemd 服务
# ------------------------------------------------------------
log "配置 systemd 服务"
install -m 644 "$ART/systemd/nsp-server.service" /etc/systemd/system/nsp-server.service
systemctl daemon-reload
systemctl enable nsp-server >/dev/null 2>&1
systemctl restart nsp-server
ok "nsp-server 已启动（systemctl status nsp-server 查看）"

# ------------------------------------------------------------
# 5. 等待健康检查
# ------------------------------------------------------------
log "等待后端就绪（最多 90 秒）"
for i in $(seq 1 45); do
    if curl -sf http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
        ok "后端已就绪：$(curl -s http://127.0.0.1:8080/actuator/health)"
        break
    fi
    if ! systemctl is-active --quiet nsp-server; then
        echo "  ✗ nsp-server 进程已退出，最近日志："
        journalctl -u nsp-server -n 40 --no-pager
        exit 1
    fi
    sleep 2
    [[ $i -eq 45 ]] && { echo "  ✗ 90 秒内未就绪，最近日志："; journalctl -u nsp-server -n 40 --no-pager; exit 1; }
done

echo
echo -e "\033[1;32m部署完成\033[0m"
echo "  用户端    http://<服务器IP>/"
echo "  商家端    http://<服务器IP>:8081/"
echo "  管理后台  http://<服务器IP>:8082/"
echo "  Swagger   http://<服务器IP>/swagger-ui.html"
