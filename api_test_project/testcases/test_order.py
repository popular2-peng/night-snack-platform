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
    """下单成功：订单落库 + 库存扣减 + 幂等键绑定（接口层 + 数据层三层验证）"""
    # ⚠️ clientToken 必须每次运行都唯一。
    # 后端用它做幂等（Redis key `order:client-token:{userId}:{token}`，TTL 10 分钟），
    # 写死会导致 10 分钟内重复运行不创建新订单、而是返回上一单。
    client_token = str(uuid.uuid4())

    # 库存基准值。
    # ⚠️ Redis 的 stock key 是「懒加载」的：只有该规格第一次被下单扣减时才从
    #    product_spec.stock 载入。所以首次运行时 key 还不存在，
    #    此时必须以 MySQL 的值作为基准，不能直接跳过断言（否则用例会静默失去校验能力）。
    before_stock = redis_helper.stock(SPEC_ID)
    if before_stock is None:
        spec_row = da_helper.query_one(
            "select stock from product_spec where id=%s", (SPEC_ID,))
        assert spec_row is not None, f"规格不存在： spec_id={SPEC_ID}"
        before_stock = spec_row["stock"]

    resp = user_client.post("/api/user/order", json_body={
        "addressId": ADDRESS_ID,
        "merchantId": MERCHANT_ID,
        "payChannel": "WECHAT_MOCK",
        "clientToken": client_token,
        "items": [{
            "productId": PRODUCT_ID,
            "specId": SPEC_ID,
            "quantity": QTY,
        }],
    })
    data = assert_success(resp)
    order_no = data["orderNo"]
    user_id = data["userId"]

    # ① 数据库：订单已落库且状态正确
    row = da_helper.query_one(
        "select status, total_amount from `order` where order_no=%s", (order_no,))
    assert row is not None, f"订单未落库： {order_no}"
    assert row["status"] == "PENDING_PAY"

    # ② Redis：库存已按购买数量预扣（注意是 QTY，不是 1）
    redis_helper.assert_stock_decreased(SPEC_ID, before_stock, QTY)

    # ③ Redis：幂等键已绑定到本次订单号
    redis_helper.assert_client_token_bound(user_id, client_token, order_no)


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
