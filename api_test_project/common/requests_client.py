"""
HTTP 请求客户端封装

对外只暴露：get / post / put / delete 四个方法（+ 一个 ApiResponse 包装类）
使用方式：
    client = ApiClient(base_url)            # 匿名客户端
    client = ApiClient(base_url, token)     # 带 token 的客户端（自动加 Bearer 头）
    resp = client.get("/api/user/info")
    assert resp.code == 0
"""
from __future__ import annotations

import json
from typing import Any

import requests

from common.logger import get_logger

logger = get_logger(__name__)

# Token 脱敏时保留的字符数（日志里只显示前缀，防止完整 token 泄漏进报告）
TOKEN_PREFIX_KEEP = 8


class ApiResponse:
    """
    统一响应包装：把 requests.Response 里我们关心的东西提取出来。

    :ivar status_code: HTTP 状态码（200/400/401/500…）
    :ivar body: 解析后的 JSON dict（非 JSON 响应则为 {"_raw": 原文本}）
    :ivar elapsed_ms: 请求耗时（毫秒，浮点）
    :ivar url: 最终请求 URL
    """

    def __init__(self, status_code: int, body: dict[str, Any], elapsed_ms: float, url: str) -> None:
        self.status_code = status_code
        self.body = body
        self.elapsed_ms = elapsed_ms
        self.url = url

    # ── 便捷属性：让用例断言更短 ──
    @property
    def code(self) -> int | None:
        """业务码（Result.code）。非标准响应返回 None。"""
        return self.body.get("code")

    @property
    def msg(self) -> str | None:
        """业务提示信息（Result.msg）"""
        return self.body.get("msg")

    @property
    def data(self) -> Any:
        """业务数据（Result.data），不存在时返回 None"""
        return self.body.get("data")

    def __repr__(self) -> str:
        return f"<ApiResponse {self.status_code} code={self.code} {self.elapsed_ms:.0f}ms>"


class ApiClient:
    """
    接口客户端：一个实例对应一个角色（自带该角色的 token）。

    为什么每个角色一个实例：
      用户/商家/管理员 的 token 互不通用，分实例比到处传 token 清晰得多。
    """

    def __init__(self, base_url: str, token: str | None = None, timeout: float = 10.0) -> None:
        self.base_url = base_url.rstrip("/")     # 防止拼出 //api/xxx
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        if token:
            self.set_token(token)

    # ── Token 管理 ──
    def set_token(self, token: str) -> None:
        """设置/更新 token（登录后调用；刷新 token 后也可重新调用）"""
        self.token = token
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def clear_token(self) -> None:
        """移除 token（测未登录场景时用）"""
        self.token = None
        self.session.headers.pop("Authorization", None)

    # ── 四个请求方法 ──
    def get(self, path: str, params: dict | None = None, **kwargs: Any) -> ApiResponse:
        return self._request("GET", path, params=params, **kwargs)

    def post(self, path: str, json_body: dict | None = None, params: dict | None = None,
             **kwargs: Any) -> ApiResponse:
        return self._request("POST", path, params=params, json_body=json_body, **kwargs)

    def put(self, path: str, json_body: dict | None = None, params: dict | None = None,
            **kwargs: Any) -> ApiResponse:
        return self._request("PUT", path, params=params, json_body=json_body, **kwargs)

    def delete(self, path: str, params: dict | None = None, **kwargs: Any) -> ApiResponse:
        return self._request("DELETE", path, params=params, **kwargs)

    # ── 核心：统一发请求 + 统一日志 + 统一包装 ──
    def _request(self, method: str, path: str, params: dict | None = None,
                 json_body: dict | None = None, **kwargs: Any) -> ApiResponse:
        url = f"{self.base_url}{path}"
        timeout = kwargs.pop("timeout", self.timeout)

        logger.info("[请求] %s %s | params=%s | body=%s | token=%s",
                    method, url, params, json_body, self._masked_token())

        try:
            resp = self.session.request(
                method=method, url=url, params=params, json=json_body,
                timeout=timeout, **kwargs
            )
        except requests.exceptions.Timeout as e:
            logger.error("[异常] %s %s 请求超时(%ss): %s", method, url, timeout, e)
            raise AssertionError(f"请求超时: {method} {url} (timeout={timeout}s)") from e
        except requests.exceptions.ConnectionError as e:
            logger.error("[异常] %s %s 连接失败: %s", method, url, e)
            raise AssertionError(f"连接失败: {method} {url}。请确认后端已启动（{self.base_url}）") from e
        except requests.exceptions.RequestException as e:
            logger.error("[异常] %s %s 请求异常: %s", method, url, e)
            raise AssertionError(f"请求异常: {method} {url} -> {e}") from e

        elapsed_ms = resp.elapsed.total_seconds() * 1000
        body = self._parse_body(resp)

        logger.info("[响应] %s %s | HTTP=%s | code=%s | %s | %.0fms | body=%s",
                    method, url, resp.status_code, body.get("code"), body.get("msg"),
                    elapsed_ms, self._truncate(body))

        return ApiResponse(resp.status_code, body, elapsed_ms, url)

    # ── 内部工具 ──
    @staticmethod
    def _parse_body(resp: requests.Response) -> dict[str, Any]:
        """把响应体解析为 dict；解析失败则包装原始文本，绝不吞掉信息"""
        try:
            parsed = resp.json()
            return parsed if isinstance(parsed, dict) else {"data": parsed, "code": None}
        except (json.JSONDecodeError, ValueError):
            resp.encoding = resp.encoding or "utf-8"
            return {"code": None, "msg": "非 JSON 响应", "_raw": resp.text}

    def _masked_token(self) -> str:
        """日志脱敏：只显示 token 前 8 位"""
        if not getattr(self, "token", None):
            return "<无>"
        return f"{self.token[:TOKEN_PREFIX_KEEP]}...({len(self.token)}位)"

    @staticmethod
    def _truncate(body: dict[str, Any], limit: int = 500) -> str:
        """日志里的响应体截断，避免超长列表刷屏"""
        text = json.dumps(body, ensure_ascii=False)
        return text if len(text) <= limit else text[:limit] + f"...(共{len(text)}字符)"
