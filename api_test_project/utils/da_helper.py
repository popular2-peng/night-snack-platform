"""
数据断言辅助（Data Assert Helper）—— 直连 MySQL 做数据库层校验

安全约定（务必遵守）：
  1. 连接口令只从 .env / 配置读取，绝不硬编码
  2. 只在测试库执行写操作；生产环境禁用本模块的 execute
  3. SQL 一律参数化（%s 占位），禁止字符串拼接（防注入 + 防引号转义问题）
  4. 连接在整个会话内复用，进程退出时自动关闭
"""
from __future__ import annotations

from typing import Any

import pymysql
from pymysql.cursors import DictCursor

from common.config_loader import load_config

_conn: pymysql.connections.Connection | None = None


def _get_conn() -> pymysql.connections.Connection:
    """懒加载单例连接（第一次调用时建立）"""
    global _conn
    if _conn is None or not _conn.open:
        cfg = load_config()["database"]
        _conn = pymysql.connect(
            host=cfg["host"],
            port=int(cfg["port"]),
            user=cfg["user"],
            password=cfg["password"],
            database=cfg["db"],
            charset="utf8mb4",
            cursorclass=DictCursor,      # 结果集直接返回 dict，不用记列顺序
            autocommit=True,             # 测试场景免手动 commit
        )
    return _conn


def close() -> None:
    """显式关闭连接（一般由 conftest 在会话结束时调用）"""
    global _conn
    if _conn and _conn.open:
        _conn.close()
    _conn = None


# ── 查询 ──
def query_one(sql: str, args: tuple | list | None = None) -> dict[str, Any] | None:
    """
    查询单行，返回 dict；查不到返回 None。

    用法：
        row = query_one("SELECT status, total_amount FROM `order` WHERE order_no=%s", (order_no,))
    """
    with _get_conn().cursor() as cur:
        cur.execute(sql, args or ())
        return cur.fetchone()


def query_all(sql: str, args: tuple | list | None = None) -> list[dict[str, Any]]:
    """查询多行，返回 list[dict]"""
    with _get_conn().cursor() as cur:
        cur.execute(sql, args or ())
        return list(cur.fetchall())


def query_value(sql: str, args: tuple | list | None = None, default: Any = None) -> Any:
    """
    查询单个标量值（COUNT/SUM 等聚合最常用）。

    用法：
        cnt = query_value("SELECT COUNT(*) AS c FROM `order` WHERE order_no=%s", (order_no,))
        注意：聚合列需要起别名，本函数取第一列的值。
    """
    row = query_one(sql, args)
    if not row:
        return default
    return next(iter(row.values()), default)


# ── 写入（造数 / 清理，仅测试库）──
def execute(sql: str, args: tuple | list | None = None) -> int:
    """
    执行 INSERT/UPDATE/DELETE，返回影响行数。

    典型用途（测试前置造数，避免走完整链路）：
        execute("UPDATE `order` SET pay_time = NOW() - INTERVAL 5 MINUTE WHERE order_no=%s", (order_no,))
    """
    with _get_conn().cursor() as cur:
        affected = cur.execute(sql, args or ())
        return affected


# ── 语义化断言（把"查 + 比"合并，用例读起来像自然语言）──
def assert_order_status(order_no: str, expect_status: str) -> None:
    """断言订单状态"""
    row = query_one("SELECT status FROM `order` WHERE order_no=%s", (order_no,))
    assert row is not None, f"订单不存在: {order_no}"
    assert row["status"] == expect_status, \
        f"订单 {order_no} 状态期望 {expect_status}，实际 {row['status']}"


def assert_payment_status(order_no: str, expect_status: str) -> None:
    """断言支付记录状态（SUCCESS / REFUNDING / REFUNDED）"""
    row = query_one(
        "SELECT status, refund_status FROM payment_record WHERE order_no=%s ORDER BY id DESC LIMIT 1",
        (order_no,),
    )
    assert row is not None, f"支付记录不存在: {order_no}"
    assert row["status"] == expect_status, \
        f"支付记录 {order_no} 状态期望 {expect_status}，实际 {row['status']}（refund_status={row['refund_status']}）"


def assert_status_log_exists(order_no: str, to_status: str) -> None:
    """断言状态机审计日志存在某次迁移（状态机用例的核心断言）"""
    cnt = query_value(
        """SELECT COUNT(*) AS c FROM order_status_log l
           JOIN `order` o ON o.id = l.order_id
           WHERE o.order_no=%s AND l.to_status=%s""",
        (order_no, to_status),
    )
    assert cnt and cnt > 0, f"订单 {order_no} 缺少状态迁移日志: to_status={to_status}"
