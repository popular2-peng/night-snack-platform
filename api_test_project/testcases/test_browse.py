"""
    商家列表/详细/菜单/搜索
"""

import pytest

from common.assertions import assert_field, assert_paged, assert_success, assert_biz_code
from common.data_loader import load_yaml


class TestBrowse:
    # ══════════════ 品类 / 商家列表 ══════════════
    @pytest.mark.p1
    @pytest.mark.browse
    def test_categories(self, anon_api, yaml_data):
        """平台品类列表：非空，且元素含 id/name"""
        case = yaml_data("browse")["categories"]
        items = assert_success(anon_api.list_categories())

        if case["expect_not_empty"]:
            assert items, "品类列表为空"
        for key in case["expect_first_keys"]:
            assert key in items[0], f"品类缺少字段 {key}"

    @pytest.mark.p1
    @pytest.mark.browse
    def test_merchant_list_paged(self, anon_api, yaml_data):
        """商家列表：分页结构正确"""
        case = yaml_data("browse")["merchant_list"]
        data = assert_success(anon_api.list_merchants(page=case["page"], size=case["size"]))

        for key in case["expect_page_keys"]:
            assert key in data, f"分页响应缺少字段 {key}"
        assert len(data["list"]) <= case["size"]

    @pytest.mark.p1
    @pytest.mark.browse
    @pytest.mark.parametrize("case", load_yaml("browse")["merchant_list_filter"], ids=[c["name"] for c in load_yaml("browse")["merchant_list_filter"]])
    def test_merchant_list_filter(self, anon_api, case):
        """商家列表筛选：按品类 / 关键词 / 未命中"""
        kwargs = {}
        if "category_id" in case:
            kwargs["category_id"] = case["category_id"]
        if "keyword" in case:
            kwargs["keyword"] = case["keyword"]

        data = assert_success(anon_api.list_merchants(**kwargs))

        if "expect_total" in case:
            assert data["total"] == case["expect_total"], \
                f"期望命中 {case['expect_total']} 条，实际 {data['total']}"
        if case.get("expect_name_contains"):
            for m in data["list"]:
                assert case["expect_name_contains"] in m["name"], \
                    f"商家[{m['name']}]不含关键词[{case['expect_name_contains']}]"

    # ══════════════ 商家详情 / 商品 ══════════════
    @pytest.mark.p2
    @pytest.mark.browse
    def test_merchant_detail_not_exist(self, anon_api, yaml_data):
        case = yaml_data("browse")["merchant_detail"]["not_exist"]
        resp = anon_api.get_merchant(case["id"])
        assert_biz_code(resp, case["expect_code"], msg="[商家不存在]")

    @pytest.mark.p1
    @pytest.mark.browse
    def test_merchant_products_only_on_sale(self, anon_api, seed_data, yaml_data):
        """商家菜单只返回上架商品"""
        case = yaml_data("browse")["merchant_products"]
        merchant = seed_data["merchants"][case["merchant_key"]]

        items = assert_paged(anon_api.list_merchant_products(merchant["id"]))
        if case["expect_all_on_sale"]:
            for p in items:
                assert p.get("status", "ON") == "ON", f"商品 {p.get('name')} 不是上架状态"

    @pytest.mark.p1
    @pytest.mark.browse
    def test_product_detail(self, anon_api, seed_data, yaml_data):
        case = yaml_data("browse")["product_detail"]
        merchant = seed_data["merchants"][case["merchant_key"]]
        product = seed_data["products"][case["product_key"]]

        data = assert_success(anon_api.get_product(merchant["id"], product["id"]))
        assert_field(data, "name", case["expect_name"])

    @pytest.mark.p2
    @pytest.mark.browse
    def test_product_detail_merchant_mismatch(self, anon_api, seed_data, yaml_data):
        """商品不属于该商家 → 3101（不能因为商品存在就放行）"""
        case = yaml_data("browse")["product_detail"]["mismatch"]
        merchant = seed_data["merchants"][case["merchant_key"]]
        product = seed_data["products"][case["product_key"]]

        resp = anon_api.get_product(merchant["id"], product["id"])
        assert_biz_code(resp, case["expect_code"], msg="[商品与商家不匹配]")

    # ══════════════ 搜索 / 分页 ══════════════

    @pytest.mark.p1
    @pytest.mark.browse
    @pytest.mark.parametrize("case", load_yaml("browse")["search_products"],
                             ids=[c["name"] for c in load_yaml("browse")["search_products"]])
    def test_search_products(self, anon_api, case):
        data = assert_success(anon_api.search_products(case["keyword"]))

        if case.get("expect_list_not_empty"):
            assert data["list"], f"关键词「{case['keyword']}」应能搜到商品"
        if "expect_total" in case:
            assert data["total"] == case["expect_total"]

    @pytest.mark.p2
    @pytest.mark.browse
    @pytest.mark.parametrize("case", load_yaml("browse")["pagination"],
                             ids=[c["name"] for c in load_yaml("browse")["pagination"]])
    def test_pagination_boundary(self, anon_api, case):
        resp = anon_api.list_merchants(page=case["page"], size=case["size"])

        if case.get("expect_code") == 1100:
            assert_biz_code(resp, 1100, msg=f"[分页参数-{case['name']}]")
        else:
            data = assert_success(resp)
            assert len(data["list"]) <= case["expect_list_size_lte"]
