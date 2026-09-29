"""
商家端接口（/api/merchant/**）

⚠️ 需要鉴权的接口基本都不传 merchantId —— 后端从 JWT 的 sub 里解析当前商家。
   这是刻意的设计（防止越权改别家数据）。唯一的例外见 list_products()。

URL 与参数依据：api.md §3.3 / §5
"""
from __future__ import annotations

from typing import Any

from api.base_api import BaseApi


class MerchantApi(BaseApi):
    prefix = "/api/merchant"

    # ══════════════ 认证与公开品类 ══════════════

    def login(self, phone: str, password: str):
        """POST /api/merchant/login —— 已封禁商家返回 7003"""
        return self._post("/login", json_body={"phone": phone, "password": password})

    def register(self, payload: dict[str, Any]):
        """
        POST /api/merchant/register —— 商家入驻申请

        body: password / phone / shopName / categoryId / description
              / logoUrl / ownerName / province / city / district / address
        """
        return self._post("/register", json_body=payload)

    def all_platform_categories(self):
        """GET /api/merchant/category/all —— 平台启用品类（入驻表单用，匿名）"""
        return self._get("/category/all")

    # ══════════════ 5.1 店铺 ══════════════

    def get_shop(self):
        """GET /api/merchant/shop —— 当前登录商家的店铺信息"""
        return self._get("/shop")

    def update_shop(self, payload: dict[str, Any]):
        """
        PUT /api/merchant/shop —— 部分更新，只传要改的字段

        可改字段：name / logoUrl / address / businessHoursStart / businessHoursEnd
                 / deliveryRadius / description / notice
        """
        return self._put("/shop", json_body=payload)

    def update_shop_status(self, status: str):
        """PUT /api/merchant/shop/status?status=OPEN|CLOSED"""
        return self._put("/shop/status", params={"status": status})

    def dashboard(self):
        """GET /api/merchant/dashboard —— 商家工作台统计"""
        return self._get("/dashboard")

    # ══════════════ 5.2 商家自定义品类（本店菜单分组）══════════════

    def list_categories(self):
        """GET /api/merchant/category —— 本店菜单分组"""
        return self._get("/category")

    def create_category(self, payload: dict[str, Any]):
        """POST /api/merchant/category —— body: {name, sort}"""
        return self._post("/category", json_body=payload)

    def update_category(self, category_id: int, payload: dict[str, Any]):
        """PUT /api/merchant/category/{id}"""
        return self._put(f"/category/{category_id}", json_body=payload)

    def delete_category(self, category_id: int):
        """DELETE /api/merchant/category/{id}"""
        return self._delete(f"/category/{category_id}")

    # ══════════════ 5.3 商品 ══════════════

    def list_products(self, merchant_id: int, page: int = 1, size: int = 20,
                      category_id: int | None = None, keyword: str | None = None,
                      status: str | None = None):
        """
        GET /api/merchant/product/list

        ⚠️ 与其它商家端接口不同，这个接口**需要显式传 merchantId**（后台管理页用）
        """
        params: dict[str, Any] = {"merchantId": merchant_id, "page": page, "size": size}
        if category_id:
            params["categoryId"] = category_id
        if keyword:
            params["keyword"] = keyword
        if status:
            params["status"] = status
        return self._get("/product/list", params=params)

    def get_product(self, product_id: int):
        """GET /api/merchant/product/{id} —— 商家后台视角（可看下架商品）"""
        return self._get(f"/product/{product_id}")

    def create_product(self, payload: dict[str, Any]):
        """POST /api/merchant/product —— body: name/categoryId/price/imageUrl/description"""
        return self._post("/product", json_body=payload)

    def update_product(self, product_id: int, payload: dict[str, Any]):
        """PUT /api/merchant/product/{id}"""
        return self._put(f"/product/{product_id}", json_body=payload)

    def update_product_status(self, product_id: int, status: str):
        """PUT /api/merchant/product/{id}/status?status=ON|OFF"""
        return self._put(f"/product/{product_id}/status", params={"status": status})

    def add_spec(self, product_id: int, name: str, stock: int,
                 price_delta: str = "0.00"):
        """
        POST /api/merchant/product/{productId}/spec

        ⚠️ priceDelta 用字符串传（金额），不要用 float
        """
        return self._post(f"/product/{product_id}/spec",
                          json_body={"name": name, "priceDelta": price_delta, "stock": stock})

    def delete_spec(self, spec_id: int):
        """DELETE /api/merchant/product/spec/{specId}"""
        return self._delete(f"/product/spec/{spec_id}")

    def delete_product(self, product_id: int):
        """DELETE /api/merchant/product/{id} —— 在售商品不可删（3105），级联删规格"""
        return self._delete(f"/product/{product_id}")

    # ══════════════ 5.4 订单流转 ══════════════

    def list_orders(self, page: int = 1, size: int = 20,
                    status: str | None = None, date_start: str | None = None,
                    date_end: str | None = None):
        """GET /api/merchant/order —— 本店订单（商家身份来自 JWT）"""
        params: dict[str, Any] = {"page": page, "size": size}
        if status:
            params["status"] = status
        if date_start:
            params["dateStart"] = date_start
        if date_end:
            params["dateEnd"] = date_end
        return self._get("/order", params=params)

    def order_stats(self):
        """GET /api/merchant/order/stats —— 各状态数量（tab 角标）"""
        return self._get("/order/stats")

    def get_order(self, order_id: int):
        """GET /api/merchant/order/{id}"""
        return self._get(f"/order/{order_id}")

    def accept_order(self, order_id: int):
        """PUT /api/merchant/order/{id}/accept —— PAID → ACCEPTED"""
        return self._put(f"/order/{order_id}/accept")

    def cooking_order(self, order_id: int):
        """PUT /api/merchant/order/{id}/cooking —— ACCEPTED → COOKING"""
        return self._put(f"/order/{order_id}/cooking")

    def complete_order(self, order_id: int):
        """PUT /api/merchant/order/{id}/complete —— COOKING → COMPLETED"""
        return self._put(f"/order/{order_id}/complete")

    def reject_order(self, order_id: int):
        """PUT /api/merchant/order/{id}/reject —— 拒单并自动退款"""
        return self._put(f"/order/{order_id}/reject")

    def refund_decision(self, order_id: int, approve: bool):
        """
        PUT /api/merchant/order/{id}/refund-decision?approve=true|false

        approve=true  → 执行渠道退款，订单变 REFUNDED
        approve=false → 回滚到申请前状态（PAID/ACCEPTED/COOKING）
        """
        return self._put(f"/order/{order_id}/refund-decision",
                         params={"approve": "true" if approve else "false"})