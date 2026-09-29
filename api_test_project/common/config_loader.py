"""
配置加载模块

职责：
  1. 确定当前运行环境（dev / test / prod）
  2. 读取 config/{env}.yaml
  3. 把 yaml 中的 ${XXX} 占位符替换为 .env / 系统环境变量中的真实值
  4. 对外提供「路径取值」与「配置对象」两种访问方式

设计原则：
  - 敏感信息（数据库/Redis 口令）只存在于 .env，绝不写进 yaml
  - 缺失的占位符不报错中断，而是保留原样并给出告警（方便本地先跑通）
"""
from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any

import yaml
from dotenv import load_dotenv

# ── 路径基准：本文件在 common/ 下，工程根 = 上一级 ──
PROJECT_ROOT = Path(__file__).resolve().parent.parent
CONFIG_DIR = PROJECT_ROOT / "config"
ENV_FILE = PROJECT_ROOT / ".env"

# ── ${XXX} 占位符正则 ──
_PLACEHOLDER_RE = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)}")

# ── 支持的环境白名单（防拼写错误静默走默认值）──
SUPPORTED_ENVS = ("dev", "test", "prod")

# 是否已初始化（避免重复读 .env / 重复读文件）
_loaded = False
_config_cache: dict[str, dict[str, Any]] = {}


def _resolve_env_name(env: str | None = None) -> str:
    """确定环境名：显式传入 > 环境变量 NSP_ENV > 默认 dev"""
    env = env or os.getenv("NSP_ENV") or "dev"
    env = env.strip().lower()
    if env not in SUPPORTED_ENVS:
        raise ValueError(f"不支持的环境: {env!r}，可选值: {SUPPORTED_ENVS}")
    return env


def _load_dotenv_once() -> None:
    """加载 .env（只做一次）。文件不存在时静默跳过，交由占位符替换阶段告警。"""
    global _loaded
    if _loaded:
        return
    if ENV_FILE.exists():
        load_dotenv(ENV_FILE, override=False)   # 不覆盖已存在的系统环境变量
    _loaded = True


def resolve_placeholders(value: Any) -> Any:
    """
    递归把配置里的 ${XXX} 替换为环境变量值。
    找不到对应环境变量时保留原样（便于定位漏配项），但打印告警。
    """
    if isinstance(value, dict):
        return {k: resolve_placeholders(v) for k, v in value.items()}
    if isinstance(value, list):
        return [resolve_placeholders(v) for v in value]
    if not isinstance(value, str):
        return value

    def _sub(match: re.Match[str]) -> str:
        key = match.group(1)
        resolved = os.getenv(key)
        if resolved is None:
            print(f"[config] 警告：占位符 ${{{key}}} 未在 .env / 环境变量中找到，保留原样")
            return match.group(0)
        return resolved

    return _PLACEHOLDER_RE.sub(_sub, value)


def load_config(env: str | None = None) -> dict[str, Any]:
    """
    加载指定环境的配置（带缓存）。

    :param env: dev / test / prod，不传则按 NSP_ENV → dev 顺序决定
    :return: 完整配置 dict
    :raises FileNotFoundError: 配置文件不存在
    """
    # 必须先加载 .env：_resolve_env_name 依赖 os.getenv("NSP_ENV")，
    # 若在加载 .env 之前解析环境名，NSP_ENV 永远读不到，会静默回退到 dev。
    _load_dotenv_once()

    env_name = _resolve_env_name(env)
    if env_name in _config_cache:
        return _config_cache[env_name]

    config_file = CONFIG_DIR / f"{env_name}.yaml"
    if not config_file.exists():
        raise FileNotFoundError(f"配置文件不存在: {config_file}")

    with open(config_file, encoding="utf-8") as f:
        raw = yaml.safe_load(f) or {}

    config = resolve_placeholders(raw)
    _config_cache[env_name] = config
    return config


def get(key_path: str, default: Any = None, env: str | None = None) -> Any:
    """
    按 "a.b.c" 路径取配置值，取不到返回 default。

    例：get("server.base_url") -> "http://localhost:8080"
        get("database.password") -> .env 里的真实口令
    """
    node: Any = load_config(env)
    for part in key_path.split("."):
        if not isinstance(node, dict) or part not in node:
            return default
        node = node[part]
    return node


if __name__ == "__main__":
    # 自测：python -m common.config_loader
    cfg = load_config()
    print("环境:", cfg["env"]["name"])
    print("base_url:", cfg["server"]["base_url"])
    print("DB口令是否已注入:", "placeholder" not in str(cfg["database"]["password"]))
