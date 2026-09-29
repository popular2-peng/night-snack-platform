"""
用户端接口（/api/user/**）
"""
from __future__ import annotations

from typing import Any

from api.base_api import BaseApi


class UserApi(BaseApi):
    prefix = "/api/user"

    # ══════════════ 认证（匿名）══════════════
    def register(self, phone: str, password: str):
        """POST /api/user/register —— 注册即登录，返回双令牌"""
        return self._post("/register", json_body={
            "phone": phone,
            "password": password
        })

    def login(self, phone: str, password: str):
        """POST /api/user/login —— 仅手机号 + 密码"""
        return self._post("/login", json_body={
            "phone": phone,
            "password": password
        })

    # ══════════════ 4.1 用户信息 ══════════════
    def info(self):
        """GET /api/user/info —— 当前登录用户信息"""
        return self._get("/info")

    # ══════════════ 4.2 收货地址 ══════════════
    def list_addresses(self):
        """GET /api/user/address —— 地址列表"""
        return self._get("/address")

    def get_address(self, address_id: int):
        """GET /api/user/address/{id} —— 越权访问他人地址返回 2101"""
        return self._get(f"/address/{address_id}")

    def create_address(self, payload: dict[str, Any]):
        """POST /api/user/address —— body: receiver/phone/detail/lat/lng/isDefault"""
        return self._post("/address", json_body=payload)

    def update_address(self, address_id: int, payload: dict[str, Any]):
        """PUT /api/user/address/{id}"""
        return self._put(f"/address/{address_id}", json_body=payload)

    def delete_address(self, address_id: int):
        """DELETE /api/user/address/{id}"""
        return self._delete(f"/address/{address_id}")

    def set_default_address(self, address_id: int):
        """PUT /api/user/address/{id}/default —— 与原默认互斥"""
        return self._put(f"/address/{address_id}/default")

    # ══════════════ 4.3 购物车（Redis Hash）══════════════
    def get_cart(self):
        """GET /api/user/cart —— 返回 CartVO{groups[], totalPrice}（按商家分组）"""
        return self._get("/cart")

    def add_to_cart(self, merchant_id: int, product_id: int,
                    quantity: int = 1, spec_id: int | None = None):
        """
        POST /api/user/cart?merchantId={merchantId}

        ⚠️ 两个易错点：
          1. merchantId 是 **query 参数**，不在请求体里
          2. specId 选填：有规格商品必传（否则 3106），无规格商品**不要传**
             （后端按 productId 走 p{productId} 的 cartKey）
        """
        body: dict[str, Any] = {
            "productId": product_id,
            "quantity": quantity
        }
        if spec_id is not None:
            body["specId"] = spec_id
        return self._post("/cart", json_body=body, params={"merchantId": merchant_id})

    def update_cart_quantity(self, cart_key: str, quantity: int):
        """
        PUT /api/user/cart —— body: {cartKey, quantity}

        cartKey 约定：有规格 = "{specId}"，无规格 = "p{productId}"（查购物车接口返回）
        """
        return self._put("/cart", json_body={
            "cartKey": cart_key,
            "quantity": quantity
        })

    def remove_cart_item(self, cart_key: str):
        """DELETE /api/user/cart/{cartKey}"""
        return self._delete(f"/cart/{cart_key}")

    def clear_cart(self):
        """DELETE /api/user/cart —— 清空当前用户购物车"""
        return self._delete("/cart")

    # ══════════════ 4.4 订单 ══════════════
    def create_order(self, payload: dict[str, Any]):
        """
        POST /api/user/order

        body: merchantId / addressId / items[{productId,specId?,quantity}]
              / payChannel / clientToken / userLat? / userLng?
        """
        return self._post("/order", json_body=payload)

    def list_orders(self, page: int = 1, size: int = 20,
                    status: str | None = None, keyword: str | None = None):
        """
        GET /api/user/order —— 我的订单分页

        status 支持单个状态码或逗号分隔多状态（如 "REFUNDING,REFUNDED"），非法值忽略
        """
        params: dict[str, Any] = {
            "page": page,
            "size": size
        }
        if status is not None:
            params["status"] = status
        return self._get("/order", params=params)

    def get_order(self, order_id: int):
        """GET /api/user/order/{id} —— 他人订单返回 4001"""
        return self._get(f"/order/{order_id}")

    def cancel_order(self, order_id: int):
        """POST /api/user/order/{id}/cancel —— 仅 PENDING_PAY 可取消"""
        return self._post(f"/order/{order_id}/cancel")

    def delete_order(self, order_id: int):
        """POST /api/user/order/{id}/delete —— 软删，列表不再显示"""
        return self._post(f"/order/{order_id}/delete")

    def apply_order(self, order_id: int):
        """
        POST /api/user/order/{id}/refund

        支付 < 1 分钟 → 即时退款（REFUNDED）
        支付 ≥ 1 分钟 → REFUNDING，等商家审批
        """
        return self._post(f"/order/{order_id}/refund")

    # ══════════════ 4.5 支付 ══════════════
    def pay(self, order_no: str, channel: str = "WECHAT_MOCK"):
        """POST /api/user/pay —— body: {orderNo, channel}"""
        return  self._post("/pay", json_body={
            "orderNo": order_no,
            "channel": channel
        })

    def pay_status(self, order_no: str):
        """GET /api/user/pay/{orderNo}/status"""
        return self._get(f"/pay/{order_no}/status")

    # ══════════════ 浏览搜索（匿名，但前缀同为 /api/user）══════════════
    def list_categories(self):
        """GET /api/user/categories —— 平台启用品类"""
        return self._get("/categories")

    def list_merchants(self, page: int = 1, size: int = 20,
                       category_id: int | None = None, keyword: str | None = None):
        """GET /api/user/merchants —— 商家列表（仅营业中）"""
        params: dict[str, Any] = {"page": page, "size": size}
        if category_id:
            params["categoryId"] = category_id
        if keyword:
            params["keyword"] = keyword
        return self._get("/merchants", params=params)

    def get_merchant(self, merchant_id: int):
        """GET /api/user/merchants/{id} —— 商家详情"""
        return self._get(f"/merchants/{merchant_id}")

    def list_merchant_products(self, merchant_id: int, page: int = 1, size: int = 20):
        """GET /api/user/merchants/{id}/products —— 仅上架商品"""
        return self._get(f"/merchants/{merchant_id}/products",
                         params={"page": page, "size": size})

    def get_product(self, merchant_id: int, product_id: int):
        """GET /api/user/merchants/{merchantId}/products/{productId}"""
        return self._get(f"/merchants/{merchant_id}/products/{product_id}")

    def search_products(self, keyword: str, page: int = 1, size: int = 20):
        """GET /api/user/search/products —— 跨商家搜索（仅上架 + 营业中）"""
        return self._get("/search/products",
                         params={"keyword": keyword, "page": page, "size": size})
