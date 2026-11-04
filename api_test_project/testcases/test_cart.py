"""
    购物车增删改查
"""

import pytest

from common.assertions import assert_biz_code, assert_success
from common.data_loader import load_yaml
from utils import redis_helper


# ══════════════ 加购 ══════════════

@pytest.mark.smoke
@pytest.mark.p1
@pytest.mark.cart
@pytest.mark.redis
def test_add_to_cart_with_spec(user_api, seed_data, yaml_data, clean_cart):
    """有规格商品：cartKey = specId"""
    case = yaml_data("cart")["add_with_spec"]
    merchant = seed_data["merchants"][case["merchant_key"]]
    product = seed_data["products"][case["product_key"]]
    spec = product["specs"][case["spec_key"]]
    user_id = seed_data["users"]["normal"]["id"]

    assert_success(user_api.add_to_cart(merchant["id"], product["id"],
                                        quantity=case["quantity"], spec_id=spec["id"]))

    redis_helper.assert_cart_qty(user_id, case["expect_cart_key"], case["quantity"])


@pytest.mark.smoke
@pytest.mark.p1
@pytest.mark.cart
@pytest.mark.redis
def test_add_to_cart_without_spec(user_api, seed_data, yaml_data, clean_cart):
    """
    无规格商品：只传 productId，cartKey = p{productId}

    ⭐ 回归用例：早期实现只按 specId 找条目，无规格商品加购会失败/串条目
    """
    case = yaml_data("cart")["add_without_spec"]
    merchant = seed_data["merchants"][case["merchant_key"]]
    product = seed_data["products"][case["product_key"]]
    user_id = seed_data["users"]["normal"]["id"]

    # 注意：这里**不传** spec_id
    assert_success(user_api.add_to_cart(merchant["id"], product["id"], quantity=case["quantity"]))

    redis_helper.assert_cart_qty(user_id, case["expect_cart_key"], case["quantity"])
    assert case["expect_cart_key"] == f"p{product['id']}", "cartKey 约定应为 p{productId}"


@pytest.mark.p1
@pytest.mark.cart
def test_add_spec_required(user_api, seed_data, yaml_data, clean_cart):
    """有规格商品不传 specId → 3106（不能静默按基础价加购）"""
    case = yaml_data("cart")["spec_required"]
    merchant = seed_data["merchants"][case["merchant_key"]]
    product = seed_data["products"][case["product_key"]]

    resp = user_api.add_to_cart(merchant["id"], product["id"], quantity=case["quantity"])
    assert_biz_code(resp, case["expect_code"], msg="[有规格商品未选规格]")


@pytest.mark.p2
@pytest.mark.cart
def test_add_spec_not_belong(user_api, seed_data, yaml_data, clean_cart):
    """specId 不属于该商品 → 3103"""
    case = yaml_data("cart")["spec_not_belong"]
    product = seed_data["products"][case["product_key"]]
    merchant_id = seed_data["merchants"]["night_bbq"]["id"]

    resp = user_api.add_to_cart(merchant_id, product["id"],
                                quantity=case["quantity"], spec_id=case["spec_id"])
    assert_biz_code(resp, case["expect_code"], msg="[规格不属于该商品]")


@pytest.mark.p2
@pytest.mark.cart
def test_add_product_not_found(user_api, seed_data, yaml_data, clean_cart):
    case = yaml_data("cart")["product_not_found"]
    merchant_id = seed_data["merchants"]["night_bbq"]["id"]

    resp = user_api.add_to_cart(merchant_id, case["product_id"], quantity=case["quantity"])
    assert_biz_code(resp, case["expect_code"], msg="[商品不存在]")


@pytest.mark.p2
@pytest.mark.cart
@pytest.mark.parametrize("case", load_yaml("cart")["quantity_boundary_list"],
                         ids=[c["name"] for c in load_yaml("cart")["quantity_boundary_list"]])
def test_add_quantity_boundary(user_api, seed_data, case, clean_cart):
    """数量边界：0/-1 → 1100；1/99 → 成功；100 → 2201"""
    merchant = seed_data["merchants"]["night_bbq"]
    product = seed_data["products"]["beef_skewer"]
    spec = product["specs"]["large"]

    resp = user_api.add_to_cart(merchant["id"], product["id"],
                                quantity=case["quantity"], spec_id=spec["id"])

    if case["expect_code"] == 0:
        assert_success(resp)
    else:
        assert_biz_code(resp, case["expect_code"], msg=f"[数量边界-{case['name']}]")


# ══════════════ 改 / 删 / 清 ══════════════

@pytest.mark.p1
@pytest.mark.cart
@pytest.mark.redis
def test_update_cart_quantity(user_api, seed_data, yaml_data, clean_cart):
    case = yaml_data("cart")["update_quantity"]
    user_id = seed_data["users"]["normal"]["id"]
    cart_key = yaml_data("cart")["add_with_spec"]["expect_cart_key"]

    # 先加购，再改数量
    assert_success(user_api.add_to_cart(
        seed_data["merchants"]["night_bbq"]["id"],
        seed_data["products"]["beef_skewer"]["id"],
        quantity=1, spec_id=4))

    assert_success(user_api.update_cart_quantity(cart_key, case["new_quantity"]))
    redis_helper.assert_cart_qty(user_id, cart_key, case["new_quantity"])


@pytest.mark.p2
@pytest.mark.cart
def test_update_cart_quantity_over_max(user_api, yaml_data, clean_cart):
    case = yaml_data("cart")["update_quantity"]
    assert_success(user_api.add_to_cart(1, 2, quantity=1, spec_id=4))

    resp = user_api.update_cart_quantity(case["cart_key"], case["over_max"])
    assert_biz_code(resp, case["over_max_expect_code"], msg="[数量超上限]")


@pytest.mark.p2
@pytest.mark.cart
def test_update_cart_missing_key(user_api, yaml_data, clean_cart):
    """不传 cartKey → 1100（历史缺陷：曾抛 500/1999）"""
    case = yaml_data("cart")["update_quantity"]["missing_cart_key"]
    resp = user_api.update_cart_quantity("", 2)
    assert_biz_code(resp, case["expect_code"], msg="[缺 cartKey]")


@pytest.mark.p1
@pytest.mark.cart
@pytest.mark.redis
def test_remove_and_clear_cart(user_api, seed_data, yaml_data, clean_cart):
    """移除单条 + 清空购物车"""
    user_id = seed_data["users"]["normal"]["id"]
    cart_key = yaml_data("cart")["add_with_spec"]["expect_cart_key"]

    assert_success(user_api.add_to_cart(1, 2, quantity=1, spec_id=4))
    assert_success(user_api.remove_cart_item(cart_key))
    assert redis_helper.cart_items(user_id).get(cart_key) is None, "条目没被移除"

    assert_success(user_api.add_to_cart(1, 2, quantity=1, spec_id=4))
    assert_success(user_api.clear_cart())
    assert not redis_helper.cart_items(user_id), "购物车没被清空"


# ══════════════ 分组 & 下架商品 ══════════════

@pytest.mark.p1
@pytest.mark.cart
def test_cart_grouped_by_merchant(user_api, seed_data, yaml_data, clean_cart):
    """跨商家加购 → 购物车按商家分组返回"""
    case = yaml_data("cart")["cross_merchant"]

    m1 = seed_data["merchants"][case["merchant_keys"][0]]
    m2 = seed_data["merchants"][case["merchant_keys"][1]]
    p1 = seed_data["products"]["beef_skewer"]
    p2 = seed_data["products"]["no_spec_dish"]      # 无规格

    assert_success(user_api.add_to_cart(m1["id"], p1["id"], quantity=1, spec_id=4))
    assert_success(user_api.add_to_cart(m2["id"], p2["id"], quantity=1))

    data = assert_success(user_api.get_cart())
    groups = data["groups"]
    assert len(groups) == case["expect_groups_len"], \
        f"期望 {case['expect_groups_len']} 个商家分组，实际 {len(groups)}"
    for g in groups:
        assert "merchantId" in g and "items" in g and g["items"]


@pytest.mark.p2
@pytest.mark.cart
def test_add_off_sale_product(user_api, merchant_api, seed_data, yaml_data, clean_cart):
    """
    加购已下架商品 → 3102

    ⚠️ 这条用例有两个坑：
       1. 数据里给的 product_id=61 属于**商家 2**，而 merchant_api 是商家 1 的 token，
          改不了别家的商品（会先返 3301）→ 所以这里改用商家 1 自己的商品
       2. 改商品状态会影响其它用例 → **必须 try/finally 恢复上架**
    """
    case = yaml_data("cart")["off_sale_product"]
    product = seed_data["products"]["chives"]        # 烤韭菜，属商家 1

    try:
        assert_success(merchant_api.update_product_status(product["id"], "OFF"))

        resp = user_api.add_to_cart(
            seed_data["merchants"]["night_bbq"]["id"], product["id"], quantity=1)
        assert_biz_code(resp, case["expect_code"], msg="[加购已下架商品]")
    finally:
        merchant_api.update_product_status(product["id"], "ON")     # 无论如何都要恢复