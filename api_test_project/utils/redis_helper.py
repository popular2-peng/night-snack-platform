"""
Redis 断言辅助 —— 直连 Redis 校验购物车 / 库存 / 幂等键

同样遵守：口令从 .env 读，Key 常量集中定义，禁止拼字符串查询。
"""
from __future__ import annotations

from typing import Any
import redis

from common.config_loader import load_config

_client: redis.Redis | None = None

# ── Key 模板（集中定义，避免各用例各写各的）──
KEY_CART = "cart:{user_id}"
KEY_STOCK = "product:stock:{sku_id}"
KEY_CLIENT_TOKEN = "order:client-token:{user_id}:{client_token}"
KEY_UPLOAD_LIMIT = "upload:sign:limit:{user_id}"


def _get_client() -> redis.Redis:
    """懒加载单例客户端（decode_responses=True：直接返回 str 而非 bytes）"""
    global _client
    if _client is None:
        cfg = load_config()["redis"]
        _client = redis.Redis(
            host=cfg["host"],
            port=int(cfg["port"]),
            db=int(cfg.get("db", 0)),
            password=cfg.get("password") or None,
            decode_responses=True,
            socket_timeout=5,
        )
    return _client


def close() -> None:
    global _client
    if _client is not None:
        _client.close()
    _client = None


# ── 购物车 ──
def cart_items(user_id: int) -> dict[str, Any]:
    """取用户购物车 Hash：{cartKey: qty}"""
    return _get_client().hgetall(KEY_CART.format(user_id=user_id))


def cart_qty(user_id: int, cart_key: str) -> int:
    """取购物车中某个 cartKey 的数量（不存在返回 0）"""
    val = _get_client().hget(KEY_CART.format(user_id=user_id), cart_key)
    return int(val) if val is not None else 0


# ── 库存 ──
def stock(sku_id: int) -> int | None:
    val = _get_client().get(KEY_STOCK.format(sku_id=sku_id))
    return int(val) if val is not None else None


# ── 幂等键 ──
def client_token_value(user_id: int, client_token: str) -> str | None:
    """取幂等键的值（期望是订单号）"""
    return _get_client().get(KEY_CLIENT_TOKEN.format(user_id=user_id, client_token=client_token))


# ── 语义化断言 ──
def assert_cart_qty(user_id: int, cart_key: str, expect_qty: int) -> None:
    """断言购物车某商品数量"""
    actual = cart_qty(user_id, cart_key)
    assert actual == expect_qty, \
        f"购物车 user={user_id} cartKey={cart_key} 期望数量 {expect_qty}，实际 {actual}"


def assert_stock_decreased(sku_id: int, before: int, qty: int) -> None:
    """断言库存扣减：当前库存 = 下单前 - 购买数量"""
    after = stock(sku_id)
    assert after is not None, f"库存 Key 不存在： sku_id={sku_id}"
    assert after == before - qty, \
        f"库存扣减异常：下单前 {before}，购买 {qty}，期望 {before - qty}，实际 {after}"


def assert_client_token_bound(user_id: int, client_token: str, expect_order_no: str) -> None:
    """断言幂等键已绑定到指定订单号"""
    actual = client_token_value(user_id, client_token)
    assert actual == expect_order_no, \
        f"幂等键 {client_token} 期望绑定 {expect_order_no}，实际 {actual}"
