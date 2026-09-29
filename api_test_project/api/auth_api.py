"""Token 刷新（/api/auth/**）"""
from api.base_api import BaseApi


class AuthApi(BaseApi):
    prefix = "/api/auth"

    def refresh(self, refresh_token: str):
        """POST /api/auth/refresh —— 用 refreshToken 换新双令牌"""
        return self._post("/refresh", json_body={"refreshToken": refresh_token})
