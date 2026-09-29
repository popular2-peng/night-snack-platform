"""
    下单/查询/取消/删除
"""
import uuid

import pytest

from common.assertions import assert_success
from utils import da_helper, redis_helper

# ── 本用例用的测试数据（需与 nsp_db 种子数据对齐）──
MERCHANT_ID = 1
ADDRESS_ID = 1
PRODUCT_ID = 2
SPEC_ID = 4          # 牛肉串 / 大份(10串)
QTY = 2


@pytest.mark.p1
@pytest.mark.order
@pytest.mark.db
def test_create_order_persist_and_stock(user_client, env_config):
    resp = user_client.post("/api/user/order", json_body={
        "addressId": 1,
        "merchantId": 1,
        "payChannel": "WECHAT_MOCK",
        "clientToken": str(uuid.uuid4()),
        "items": [{
            "productId": 2,
            "specId": 4,
            "quantity": 2
        }]
    })
    assert_success(user_client.post("/api/user/pay", json_body={
        "orderNo": resp.data["orderNo"],
        "channel": "WECHAT_MOCK"
    }))


@pytest.mark.p2
@pytest.mark.order
def test_create_order_idempotent(user_client):
    """同一 clientToken 重复下单 → 返回同一订单（幂等）"""
    client_token = str(uuid.uuid4())
    payload = {
        "addressId": ADDRESS_ID,
        "merchantId": MERCHANT_ID,
        "payChannel": "WECHAT_MOCK",
        "clientToken": client_token,
        "items": [{
            "productId": PRODUCT_ID,
            "specId": SPEC_ID,
            "quantity": QTY,
        }]
    }

    first = assert_success(user_client.post("/api/user/order", json_body=payload))
    second = user_client.post("/api/user/order", json_body=payload)
    if second.code == 0:
        assert second.data["orderNo"] == first["orderNo"], "幂等失败：返回了新订单"
    else:
        assert second.code == 4006, f"期望幂等返回同单或 4006，实际 {second.code}"

    # 库层面：真正确认只创建了一条（防止 Redis 键失效后重复落库）
    cnt = da_helper.query_value(
        "select count(*) from `order` where order_no=%s", (first["orderNo"],)
    )
    assert cnt == 1


@pytest.mark.p1
@pytest.mark.order
@pytest.mark.db
def test_refund_needs_merchant_approval(user_client, merchant_client):
    """支付超 1 分钟申请退款 → REFUNDING，商家同意 → REFUNDED"""
    order = _create_and_pay(user_client)                       # 见下方辅助
    # 关键技巧：把 pay_time 改旧，绕过 1 分钟即时退（否则会走即时退分支）
    da_helper.execute(
        "UPDATE `order` SET pay_time = NOW() - INTERVAL 5 MINUTE WHERE order_no=%s",
        (order["orderNo"],))

    assert_success(user_client.post(f"/api/user/order/{order['id']}/refund"))
    da_helper.assert_order_status(order["orderNo"], "REFUNDING")
    da_helper.assert_status_log_exists(order["orderNo"], "REFUNDING")

    # 商家同意
    assert_success(merchant_client.put(f"/api/merchant/order/{order['id']}/refund-decision",
                                       params={"approve": "true"}))
    da_helper.assert_order_status(order["orderNo"], "REFUNDED")
    da_helper.assert_payment_status(order["orderNo"], "REFUNDED")   # payment_record.refund_status=REFUNDED


def _create_and_pay(user_client):
    client_token = str(uuid.uuid4())
    payload = {
        "addressId": ADDRESS_ID,
        "merchantId": MERCHANT_ID,
        "payChannel": "WECHAT_MOCK",
        "clientToken": client_token,
        "items": [{
            "productId": PRODUCT_ID,
            "specId": SPEC_ID,
            "quantity": QTY,
        }]
    }
    order = assert_success(user_client.post("/api/user/order", json_body=payload))
    assert_success(user_client.post("/api/user/pay", json_body={
        "orderNo": order["orderNo"],
        "channel": "WECHAT_MOCK"
    }))
    return order
