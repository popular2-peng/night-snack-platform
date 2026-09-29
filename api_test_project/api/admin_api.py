"""
平台管理接口（/api/admin/**）

URL 与参数依据：api.md §3.4 / §6

⚠️ api.md 里存在同一业务的两代接口（V1 / V2），例如：
   · 商家封禁：PUT /merchants/{id}/suspend（V1） 与 PUT /merchant/{id}/ban（V2）
   · 入驻审批：PUT /merchant-application/{id}/approve（V1）
              与 POST /application/{id}/approve（V2）
   本类两套都封装（方便做兼容性回归），但用例里**默认用 V2**，注释已标明。
"""
from __future__ import annotations

from typing import Any

from api.base_api import BaseApi


class AdminApi(BaseApi):
    prefix = "/api/admin"

    # ══════════════ 认证 ══════════════

    def login(self, username: str, password: str):
        """POST /api/admin/login —— 管理员用 username 登录（不是手机号）"""
        return self._post("/login", json_body={"username": username, "password": password})

    # ══════════════ 6.1 看板 ══════════════

    def dashboard_stats(self):
        """GET /api/admin/dashboard/stats —— 核心统计卡片"""
        return self._get("/dashboard/stats")

    def dashboard_order_stats(self, date: str | None = None):
        """GET /api/admin/dashboard/order-stats?date=yyyy-MM-dd（不传=全量）"""
        params = {"date": date} if date else None
        return self._get("/dashboard/order-stats", params=params)

    def dashboard_recent_orders(self):
        """GET /api/admin/dashboard/recent-orders"""
        return self._get("/dashboard/recent-orders")

    def dashboard_user_trend(self):
        """GET /api/admin/dashboard/user-trend —— 近 N 日注册趋势"""
        return self._get("/dashboard/user-trend")

    def dashboard_application_trend(self):
        """GET /api/admin/dashboard/application-trend —— 近 N 日入驻趋势"""
        return self._get("/dashboard/application-trend")

    # ══════════════ 6.2 入驻审核 ══════════════

    def pending_applications(self, page: int = 1, size: int = 20,
                             keyword: str | None = None, status: str | None = None,
                             category_id: int | None = None):
        """GET /api/admin/merchant-application/pending"""
        params: dict[str, Any] = {"page": page, "size": size}
        if keyword:
            params["keyword"] = keyword
        if status:
            params["status"] = status
        if category_id:
            params["categoryId"] = category_id
        return self._get("/merchant-application/pending", params=params)

    def application_detail(self, application_id: int):
        """GET /api/admin/merchant-application/{id}/detail"""
        return self._get(f"/merchant-application/{application_id}/detail")

    def approve_application(self, application_id: int, payload: dict[str, Any] | None = None):
        """POST /api/admin/application/{id}/approve —— V2（推荐）"""
        return self._post(f"/application/{application_id}/approve",
                          json_body=payload or {})

    def reject_application(self, application_id: int, reject_reason: str):
        """
        POST /api/admin/application/{id}/reject —— V2（推荐）

        ⚠️ rejectReason 有 @NotBlank，批量版还有 @Size(min=10, max=200) ——
           太短会直接 1100，写用例数据时注意 10 字以上
        """
        return self._post(f"/application/{application_id}/reject",
                          json_body={"rejectReason": reject_reason})

    # —— V1 版入驻审批（api.md 里保留的老接口，测兼容性时用）——

    def approve_application_v1(self, application_id: int):
        """PUT /api/admin/merchant-application/{id}/approve —— V1"""
        return self._put(f"/merchant-application/{application_id}/approve")

    def reject_application_v1(self, application_id: int, reject_reason: str):
        """PUT /api/admin/merchant-application/{id}/reject —— V1"""
        return self._put(f"/merchant-application/{application_id}/reject",
                         json_body={"rejectReason": reject_reason})

    def batch_approve_applications(self, ids: list[int]):
        """POST /api/admin/merchant-application/batch-approve"""
        return self._post("/merchant-application/batch-approve", json_body={"ids": ids})

    def batch_reject_applications(self, ids: list[int], reject_reason: str):
        """POST /api/admin/merchant-application/batch-reject"""
        return self._post("/merchant-application/batch-reject",
                          json_body={"ids": ids, "rejectReason": reject_reason})

    # ══════════════ 6.3 用户管理 ══════════════

    def list_users(self, page: int = 1, size: int = 20, keyword: str | None = None,
                   status: str | None = None, date_start: str | None = None,
                   date_end: str | None = None):
        """GET /api/admin/users —— 支持关键词 / 状态 / 注册日期区间"""
        params: dict[str, Any] = {"page": page, "size": size}
        for k, v in (("keyword", keyword), ("status", status),
                     ("dateStart", date_start), ("dateEnd", date_end)):
            if v:
                params[k] = v
        return self._get("/users", params=params)

    def user_detail(self, user_id: int):
        """GET /api/admin/users/{id}/detail"""
        return self._get(f"/users/{user_id}/detail")

    def ban_user(self, user_id: int, reason: str, duration_days: int | None = None):
        """
        PUT /api/admin/users/{id}/ban

        · reason 不能为空（否则 2302）
        · durationDays 不能为负（否则 2309）；不传代表永久封禁
        """
        body: dict[str, Any] = {"reason": reason}
        if duration_days is not None:
            body["durationDays"] = duration_days
        return self._put(f"/users/{user_id}/ban", json_body=body)

    def unban_user(self, user_id: int):
        """PUT /api/admin/users/{id}/unban —— 未封禁用户解封返回 2304"""
        return self._put(f"/users/{user_id}/unban")

    def delete_user(self, user_id: int):
        """DELETE /api/admin/users/{id} —— 仅封禁账号可删（启用返回 2306）"""
        return self._delete(f"/users/{user_id}")

    def user_logs(self, user_id: int):
        """GET /api/admin/users/{id}/logs —— 操作日志"""
        return self._get(f"/users/{user_id}/logs")

    def batch_unban_users(self, ids: list[int]):
        """POST /api/admin/users/batch-unban"""
        return self._post("/users/batch-unban", json_body={"ids": ids})

    def batch_delete_users(self, ids: list[int]):
        """POST /api/admin/users/batch-delete —— 启用账号自动跳过"""
        return self._post("/users/batch-delete", json_body={"ids": ids})

    # ══════════════ 6.4 商家管理 ══════════════

    def list_merchants(self, page: int = 1, size: int = 20,
                       keyword: str | None = None, status: str | None = None):
        """GET /api/admin/merchants —— V1 列表"""
        params: dict[str, Any] = {"page": page, "size": size}
        if keyword:
            params["keyword"] = keyword
        if status:
            params["status"] = status
        return self._get("/merchants", params=params)

    def merchant_stats(self):
        """GET /api/admin/merchant/stats —— 商家统计概览"""
        return self._get("/merchant/stats")

    def merchant_detail(self, merchant_id: int):
        """GET /api/admin/merchant/{id}/detail —— V2 详情"""
        return self._get(f"/merchant/{merchant_id}/detail")

    def ban_merchant(self, merchant_id: int, reason: str):
        """PUT /api/admin/merchant/{id}/ban —— V2 封禁（reason 为空 → 7002）"""
        return self._put(f"/merchant/{merchant_id}/ban", json_body={"reason": reason})

    def unban_merchant(self, merchant_id: int):
        """PUT /api/admin/merchant/{id}/unban —— V2 解封（未封禁 → 7004）"""
        return self._put(f"/merchant/{merchant_id}/unban")

    def pause_merchant(self, merchant_id: int):
        """PUT /api/admin/merchant/{id}/pause —— 暂停营业"""
        return self._put(f"/merchant/{merchant_id}/pause")

    def resume_merchant(self, merchant_id: int):
        """PUT /api/admin/merchant/{id}/resume —— 恢复营业"""
        return self._put(f"/merchant/{merchant_id}/resume")

    def suspend_merchant(self, merchant_id: int):
        """PUT /api/admin/merchants/{id}/suspend —— V1 封禁（保留做兼容用例）"""
        return self._put(f"/merchants/{merchant_id}/suspend")

    def activate_merchant(self, merchant_id: int):
        """PUT /api/admin/merchants/{id}/activate —— V1 解封（保留做兼容用例）"""
        return self._put(f"/merchants/{merchant_id}/activate")

    def delete_merchant(self, merchant_id: int):
        """DELETE /api/admin/merchant/{id} —— 仅封禁可删（未封禁 → 2307）"""
        return self._delete(f"/merchant/{merchant_id}")

    def batch_delete_merchants(self, ids: list[int]):
        """POST /api/admin/merchant/batch-delete"""
        return self._post("/merchant/batch-delete", json_body={"ids": ids})

    def merchant_logs(self, merchant_id: int):
        """GET /api/admin/merchant/{id}/logs"""
        return self._get(f"/merchant/{merchant_id}/logs")

    # ══════════════ 6.5 品类管理 ══════════════

    def list_categories(self, page: int = 1, size: int = 20,
                        keyword: str | None = None, status: str | None = None):
        """GET /api/admin/category —— 支持关键词 / 状态筛选"""
        params: dict[str, Any] = {"page": page, "size": size}
        if keyword:
            params["keyword"] = keyword
        if status:
            params["status"] = status
        return self._get("/category", params=params)

    def all_categories(self):
        """GET /api/admin/category/all —— 全部启用品类"""
        return self._get("/category/all")

    def category_stats(self):
        """GET /api/admin/category/stats"""
        return self._get("/category/stats")

    def category_detail(self, category_id: int):
        """GET /api/admin/category/{id}"""
        return self._get(f"/category/{category_id}")

    def create_category(self, payload: dict[str, Any]):
        """
        POST /api/admin/category

        body: name（2-10 字，必填）/ icon / iconType / sortOrder / status / remark
        """
        return self._post("/category", json_body=payload)

    def update_category(self, category_id: int, payload: dict[str, Any]):
        """PUT /api/admin/category/{id}"""
        return self._put(f"/category/{category_id}", json_body=payload)

    def enable_category(self, category_id: int):
        """PUT /api/admin/category/{id}/enable"""
        return self._put(f"/category/{category_id}/enable")

    def disable_category(self, category_id: int):
        """PUT /api/admin/category/{id}/disable"""
        return self._put(f"/category/{category_id}/disable")

    def delete_category(self, category_id: int):
        """DELETE /api/admin/category/{id} —— 有商家关联时不可删（8005）"""
        return self._delete(f"/category/{category_id}")

    def reorder_categories(self, items: list[dict[str, Any]]):
        """
        POST /api/admin/category/reorder

        body: [{"id": 1, "sortOrder": 1}, {"id": 2, "sortOrder": 2}]
        """
        return self._post("/category/reorder", json_body=items)

    def batch_enable_categories(self, ids: list[int]):
        """POST /api/admin/category/batch-enable"""
        return self._post("/category/batch-enable", json_body={"ids": ids})

    def batch_disable_categories(self, ids: list[int]):
        """POST /api/admin/category/batch-disable"""
        return self._post("/category/batch-disable", json_body={"ids": ids})

    def batch_delete_categories(self, ids: list[int]):
        """POST /api/admin/category/batch-delete"""
        return self._post("/category/batch-delete", json_body={"ids": ids})

    # ══════════════ 6.6 订单管理 ══════════════

    def list_orders(self, page: int = 1, size: int = 20,
                    keyword: str | None = None, status: str | None = None):
        """GET /api/admin/orders —— keyword 匹配订单号/用户名/商家名"""
        params: dict[str, Any] = {"page": page, "size": size}
        if keyword:
            params["keyword"] = keyword
        if status:
            params["status"] = status
        return self._get("/orders", params=params)

    def order_detail(self, order_id: int):
        """GET /api/admin/orders/{id}"""
        return self._get(f"/orders/{order_id}")

    def refund_order(self, order_id: int):
        """POST /api/admin/orders/{id}/refund —— 管理员发起退款"""
        return self._post(f"/orders/{order_id}/refund")

    def batch_refund_orders(self, ids: list[int]):
        """POST /api/admin/orders/batch-refund"""
        return self._post("/orders/batch-refund", json_body={"ids": ids})

    def batch_delete_orders(self, ids: list[int]):
        """POST /api/admin/orders/batch-delete"""
        return self._post("/orders/batch-delete", json_body={"ids": ids})