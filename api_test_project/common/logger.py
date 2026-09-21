"""
日志模块封装

用法：
    from common.logger import get_logger
    logger = get_logger(__name__)
    logger.info("下单成功 orderNo=%s", order_no)

输出：
    控制台（受 pytest -s / log_cli 控制可见）
    + logs/run-YYYY-MM-DD.log（始终落盘）
"""
from __future__ import annotations

import logging
import sys
from datetime import datetime
from pathlib import Path

# ── 日志目录：工程根/logs ──
LOG_DIR = Path(__file__).resolve().parent.parent / "logs"
LOG_DIR.mkdir(exist_ok=True)

_LOG_FORMAT = "%(asctime)s [%(levelname)-5s] [%(name)s] %(message)s"
_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

# 已创建的 logger 缓存（避免重复添加 handler 导致日志重复输出 N 遍）
_loggers: dict[str, logging.Logger] = {}


def _build_file_handler() -> logging.FileHandler:
    """按日式落盘的文件处理器"""
    log_file = LOG_DIR / f"run-{datetime.now():%y-%m-%d}.log"
    handler = logging.FileHandler(log_file, encoding="utf-8")
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(logging.Formatter(_LOG_FORMAT, datefmt=_DATE_FORMAT))
    return handler


def _build_console_handler() -> logging.StreamHandler:
    """控制台处理器(stdout，配置pytest -s 实时可见)"""
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.INFO)
    handler.setFormatter(logging.Formatter(_LOG_FORMAT, datefmt=_DATE_FORMAT))
    return handler


def get_logger(name: str, level: int = logging.DEBUG) -> logging.Logger:
    """
    获取（或创建）一个带统一格式的 logger。

    :param name: 一般传 __name__，日志里就能看出是哪个模块打的
    :param level: 该 logger 的级别（默认 DEBUG：文件留全量，控制台按 handler 级别过滤）
    """
    if name in _loggers:
        return _loggers[name]

    logger = logging.getLogger(name)
    logger.setLevel(level)
    logger.propagate = False        # 不再向 root 传播，防止被 pytest 的 handler 重复打印

    # 只在没有 handler 时添加（防止模块被多次 import 时重复挂）
    if not logger.handlers:
        logger.addHandler(_build_file_handler())
        logger.addHandler(_build_console_handler())

    _loggers[name] = logger
    return logger

