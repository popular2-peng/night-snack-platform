"""
    下单/查询/取消/删除
"""
import uuid

import pytest

from common.assertions import assert_success, assert_biz_code
from common.data_loader import load_yaml
from utils import da_helper, redis_helper
from utils.data_generator import gen_client_token


class TestOrder:
    @pytest.mark.smoke
    @pytest.mark.p1
    @pytest.mark.order
    @pytest.mark.db
    @pytest.mark.redis
    def test_create_order_persist_and_stock(self, user_api, order_payload, yaml_data, seed_data):
        """下单成功：订单落库 + 库存扣减 + 幂等键绑定（三层校验）"""
        case = yaml_data("order")["create_success"]
        spec = seed_data["products"]["beef_skewer"]["specs"]["large"]
        quantity = case["items"][0]["quantity"]

        # 库存基准：Redis key 是懒加载的，首次运行时回落到 MySQL
        before_stock = redis_helper.stock(spec["id"])
        if before_stock is None:
            before_stock = da_helper.query_value(
                "select stock from product_spec where id=%s", (spec["id"],)
            )
        assert before_stock is not None, f"规格不存在：spec_id={spec['id']}"

        payload = order_payload(case)
        data = assert_success(user_api.create_order(payload))
        order_no, user_id = data["orderNo"], data["userId"]

        # ① 数据库：订单已落库且状态/金额正确
        row = da_helper.query_one(
            "select status, total_amount from `order` where order_no=%s", (order_no,)
        )
        assert row is not None, f"订单未落库：{order_no}"
        assert row["status"] == case["expect"]["status"], \
            f"订单状态期望 {case['expect']['status']}，实际 {row['status']}"
        assert str(row["total_amount"]) == case["expect"]["total_amount"], \
            f"订单金额期望 {case['expect']['total_amount']}，实际 {row['total_amount']}"

        # ② Redis：库存按购买数量扣减
        redis_helper.assert_stock_decreased(spec["id"], before_stock, quantity)
        # ③ Redis：幂等键已绑定到本次订单号
        redis_helper.assert_client_token_bound(user_id, payload["clientToken"], order_no)

    @pytest.mark.p2
    @pytest.mark.order
    def test_create_order_without_spec(self, user_api, seed_data, yaml_data):
        """无规格商品下单：items 里不带 specId"""
        case = yaml_data("order")["create_no_spec"]
        merchant = seed_data["merchants"][case["merchant_key"]]  # 商家 2（辣翻天）
        product = seed_data["products"]["no_spec_dish"]  # 商品 61（无规格）
        addr = seed_data["addresses"]["shuangjing_default"]  # ⭐ 双井：与商家 2 坐标相同

        payload = {
            "merchantId": merchant["id"],
            "addressId": addr["id"],
            "payChannel": case["pay_channel"],
            "clientToken": gen_client_token(),
            "items": [{"productId": product["id"], "quantity": case["items"][0]["quantity"]}],
        }
        data = assert_success(user_api.create_order(payload))

        assert data["status"] == case["expect"]["status"]
        assert data["items"][0].get("specId") is None, "无规格商品的下单明细不应带 specId"

    BAN_CASES = load_yaml("order")["bad_list"]

    @pytest.mark.p1
    @pytest.mark.order
    @pytest.mark.parametrize("case", BAN_CASES, ids=[c["name"] for c in BAN_CASES])
    def test_create_order_failed(self, user_api, order_payload, case):
        """下单失败矩阵：不同维度的参数/业务校验都要拦住"""
        drop = ("merchantId", ) if case["name"] == "merchant_id_missing" else ()
        payload = order_payload(case, drop=drop)

        resp = user_api.create_order(payload)
        assert_biz_code(resp, case["expect_code"], msg=f"[{case['title']}]")

    @pytest.mark.p1
    @pytest.mark.order
    @pytest.mark.db
    def test_create_order_idempotent(self, user_api, order_payload, yaml_data):
        """同一 clientToken 重复提交 → 同一订单号，且库里只有一条"""
        case = yaml_data("order")["idempotent"]
        payload = order_payload(case)

        first = assert_success(user_api.create_order(payload))
        second = user_api.create_order(payload)

        if second.code == 0:
            assert second.data["orderNo"] == first["orderNo"], "幂等失败：返回了新订单"
        else:
            assert second.code == 4006, f"期望幂等返回同单或 4006，实际 {second.code}"

        cnt = da_helper.query_value(
            "select count(*) from `order` where order_no=%s", (first["orderNo"],))
        assert cnt == 1, f"同一 clientToken 落库了 {cnt} 条订单"

    LIST_CASES = load_yaml("order")["list_filters"]

    @pytest.mark.p1
    @pytest.mark.order
    @pytest.mark.parametrize("case", LIST_CASES, ids=[c["name"] for c in LIST_CASES])
    def test_order_list_filters(self, user_api, scenes, case):
        # 先造一笔待支付订单，保证「按状态筛」有数据可筛
        scenes["create_pending_order"]()

        kwargs = {}
        if "status" in case:
            kwargs["status"] = case["status"]
        if "keyword" in case:
            kwargs["keyword"] = case["keyword"]

        resp = user_api.list_orders(**kwargs)
        if case.get("expect_code") == 0:
            data = assert_success(resp)
        else:
            data = assert_success(resp)

        if case.get("expect_all_match") and case.get("status") in ("PENDING_PAY",):
            for o in data["list"]:
                assert o["status"] == case["status"], f"筛选失效：{o['status']}"

    @pytest.mark.p1
    @pytest.mark.order
    @pytest.mark.db
    @pytest.mark.redis
    def test_cancel_order_releases_stock(self,user_api, order_payload, yaml_data, seed_data):
        """取消待支付订单 → CANCELLED，且预扣库存被释放"""
        case = yaml_data("order")["cancel"]
        spec = seed_data["products"]["beef_skewer"]["specs"]["large"]
        quantity = yaml_data("order")["create_success"]["items"][0]["quantity"]

        before = redis_helper.stock(spec["id"])
        if before is None:
            before = da_helper.query_value("select stock from `product_spec` where id%s", (spec["id"],))

        order = assert_success(user_api.create_order(order_payload(
            yaml_data("order")["create_success"]
        )))
        redis_helper.assert_stock_decreased(spec["id"], before, quantity)

        assert_success(user_api.cancel_order(order["id"]))
        da_helper.assert_order_status(order["orderNo"], case["expect_status"])
        if case["expect_stock_released"]:
            assert redis_helper.stock(spec["id"]) == before, \
                f"取消后库存没释放：期望{before}, 实际{redis_helper.stock(spec['id'])}"

    @pytest.mark.p2
    @pytest.mark.order
    @pytest.mark.db
    def test_delete_order_soft(self, user_api, order_payload, yaml_data):
        """删除订单是软删：列表看不到，但 DB 里记录还在"""
        case = yaml_data("order")["create_success"]
        order = assert_success(user_api.create_order(order_payload(case)))

        assert_success(user_api.delete_order(order["id"]))

        data = assert_success(user_api.list_orders(page=1, size=50))
        assert all(o["orderNo"] != order["orderNo"] for o in data["list"]), \
            f"已删除的订单仍然出现在列表中"

        row = da_helper.query_one(
            "select user_deleted from `order` where order_no=%s", (order["orderNo"],)
        )
        assert row["user_deleted"] == 1, "应该是软删(user_delete=1)而不是物理删除"

    @pytest.mark.smoke
    @pytest.mark.p1
    @pytest.mark.payment
    @pytest.mark.db
    def test_pay_wechat_mock(self, user_api, scenes, yaml_data):
        case = yaml_data("payment")["pay_wechat_mock"]
        order = scenes[case["prepare"]]()          # create_pending_order

        assert_success(user_api.pay(order["orderNo"], case["channel"]))

        da_helper.assert_order_status(order["orderNo"], case["expect_order_status"])
        da_helper.assert_payment_status(order["orderNo"], "SUCCESS")

    @pytest.mark.p2
    @pytest.mark.payment
    def test_pay_alipay_fallback(self, user_api, scenes, yaml_data):
        """支付宝：沙箱未配置时降级 mock 直接成功，配了就返回二维码"""
        case = yaml_data("payment")["pay_alipay"]
        order = scenes[case["prepare"]]()

        data = assert_success(user_api.pay(order["orderNo"], case["channel"]))

        if case["expect_qr_code_or_null"]:
            qr = data.get("qrCode") if isinstance(data, dict) else None
            assert qr is None or isinstance(qr, str), "qrCode 类型异常"

    @pytest.mark.p1
    @pytest.mark.payment
    def test_pay_status_query(self, user_api, scenes, yaml_data):
        case = yaml_data("payment")["pay_status_query"]
        order = scenes[case["prepare"]]()

        data = assert_success(user_api.pay_status(order["orderNo"]))

        assert data["status"] == case["expect_status"], \
            f"支付状态期望 {case['expect_status']}，实际 {data['status']}"

    PAY_BAD = load_yaml("payment")["bad_list"]

    @pytest.mark.p2
    @pytest.mark.payment
    @pytest.mark.parametrize("case", PAY_BAD, ids=[c["name"] for c in PAY_BAD])
    def test_pay_failed(self, user_api, scenes, case):
        if "prepare" in case:
            order = scenes[case["prepare"]]()
            order_no = order["orderNo"]
        else:
            order_no = case.get("order_no", "NSP99999999999999")

        resp = user_api.pay(order_no, case["channel"])
        assert_biz_code(resp, case["expect_code"], msg=f"[{case['title']}]")

    @pytest.mark.skip(reason="需要真实支付宝沙箱密钥才能构造合法回调签名")
    @pytest.mark.p2
    @pytest.mark.payment
    @pytest.mark.db
    def test_alipay_callback_idempotent(self, pay_api, user_api, scenes, yaml_data):
        """同一 out_trade_no 连发两次回调 → 只处理一次"""
        case = yaml_data("payment")["callback_idempotent"]
        order = scenes["create_pending_order"]()

        form = {"out_trade_no": order["orderNo"], "trade_status": "TRADE_SUCCESS"}
        pay_api.alipay_callback(form)
        pay_api.alipay_callback(form)

        cnt = da_helper.query_value(
            "select count(*) from payment_record where order_no=%s", (order["orderNo"],))
        assert cnt == case["expect_payment_record_count"], \
            f"回调重复处理了：payment_record 有 {cnt} 条"
        da_helper.assert_order_status(order["orderNo"], case["expect_order_status"])
