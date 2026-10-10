"""
业务接口层基类

设计要点：
  1. 只持有 ApiClient（负责 URL 拼接与发送），不管鉴权 —— 鉴权由 client 上的 token 决定
  2. 用 prefix 类属性声明 URL 前缀，子类只写「前缀之后」的部分，避免重复
  3. 返回统一的 ApiResponse，由调用方（用例）决定怎么断言
"""
from __future__ import annotations

from typing import Any

from common.requests_client import ApiClient, ApiResponse


class BaseApi:
    """业务接口类父类：一个类对应一个 URL 前缀"""

    #: URL 前缀，由子类覆盖，如 "/api/user"
    prefix: str = ""

    def __init__(self, client: ApiClient) -> None:
        self.client = client

    def _url(self, path: str) -> str:
        """拼出完整路径：prefix + path"""
        return f"{self.prefix}{path}"

    # ── 便捷转发（子类里少写一层 self.client，可读性更好）──
    def _get(self, path: str, **kw: Any) -> ApiResponse:
        return self.client.get(self._url(path), **kw)

    def _post(self, path: str = "", **kw: Any) -> ApiResponse:
        return self.client.post(self._url(path), **kw)

    def _put(self, path: str, **kw: Any) -> ApiResponse:
        return self.client.put(self._url(path), **kw)

    def _delete(self, path: str = "", **kw: Any) -> ApiResponse:
        return self.client.delete(self._url(path), **kw)

    # ── 通用入口：给越权矩阵 / 冒烟巡检这类「端点即数据」的用例用 ──
    # ⚠️ 与上面四个不同：这里的 path 是**完整路径**，**不再**拼 self.prefix
    #    （原因和踩坑记录见第七节）
    def request(self, method: str, path: str, **kw: Any) -> ApiResponse:
        if not path.startswith("/"):
            raise ValueError(f"request() 需要完整路径（以 / 开头），实际收到 {path!r}")
        return getattr(self.client, method.lower())(path, **kw)
