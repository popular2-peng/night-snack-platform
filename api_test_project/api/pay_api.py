"""支付回调（/api/pay/**）—— 依据 api.md §3.9"""
from __future__ import annotations

from typing import Any

from api.base_api import BaseApi


class PayApi(BaseApi):
    prefix = "/api/pay"

    def alipay_callback(self, form: dict[str, Any] | None = None):
        """
        POST /api/pay/alipay/callback —— 支付宝异步通知（匿名，需验签）

        ⚠️ 真实支付宝回调是 form 表单（application/x-www-form-urlencoded），
           不是 JSON —— 用 data= 而不是 json_body=
        """
        # 注意：这里用 **data=** 传表单，不是 json_body=，所以走 _post 但换掉参数名
        return self._post("/alipay/callback", data=form or {})