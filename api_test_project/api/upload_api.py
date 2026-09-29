"""文件上传（/api/upload/**）—— 依据 api.md §7"""
from __future__ import annotations

from api.base_api import BaseApi


class UploadApi(BaseApi):
    prefix = "/api/upload"

    def sign(self, biz_type: str, file_name: str,
             target_id: int | None = None, user_id: int | None = None):
        """
        GET /api/upload/sign

        ⚠️ 这个接口是**匿名**的（商家入驻上传 LOGO 时还没登录），
           所以用 anon_client 或登录 client 调都可以
        ⚠️ fileName 扩展名必须是 jpg/jpeg/png/webp 之一，否则 6002
        """
        params = {"bizType": biz_type, "fileName": file_name}
        if target_id is not None:
            params["targetId"] = target_id
        if user_id is not None:
            params["userId"] = user_id
        return self._get("/sign", params=params)

    def bind(self, oss_key: str, biz_type: str, url: str, target_id: int | None = None):
        """POST /api/upload/bind —— body: ossKey/bizType/url/targetId"""
        body = {"ossKey": oss_key, "bizType": biz_type, "url": url}
        if target_id is not None:
            body["targetId"] = target_id
        return self._post("/bind", json_body=body)

    def file_url(self, oss_key: str):
        """GET /api/upload/url?ossKey=xxx —— 私有桶的临时访问 URL"""
        return self._get("/url", params={"ossKey": oss_key})

    def callback(self, payload: dict | None = None):
        """POST /api/upload/callback —— OSS 异步回调（匿名，验签失败 6004）"""
        return self._post("/callback", json_body=payload or {})