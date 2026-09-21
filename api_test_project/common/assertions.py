"""
自定义断言库：把接口测试里高频的断言模式收口，便于统一报错信息与后续增强。
"""
from __future__ import annotations

from typing import Any

from common.requests_client import ApiResponse


def assert_success(resp: ApiResponse, msg: str = "") -> Any:
    """
    断言「HTTP 200 + 业务码 0」，返回 data 供后续链式使用。

    用法：
        data = assert_success(resp)
        order_id = data["id"]
    """
    prefix = f"{msg} " if msg else ""
    assert resp.status_code == 200, \
        f"{prefix}期望 HTTP 200，实际 {resp.status_code}｜body={resp.body}"
    assert resp.code == 0, \
        f"{prefix}期望业务码 0，实际 {resp.code}｜msg={resp.msg}｜body={resp.body}"
    return resp.data


def assert_biz_code(resp: ApiResponse, expect_code: int, msg: str = "") -> ApiResponse:
    """
       断言业务码等于期望值（负向用例主力）。

       用法：
           assert_biz_code(resp, 2004)            # 密码错误
       """
    prefix = f"{msg} " if msg else ""
    assert resp.code == expect_code, \
        f"{prefix}期望业务码 {expect_code}，实际 {resp.code}｜msg={resp.msg}｜body={resp.body}"
    return resp


def assert_unauthorized(resp: ApiResponse, expect_code: int) -> None:
    """断言未登录/无权限：HTTP 401 + 指定业务码"""
    assert resp.status_code == 401, \
        f"期望 HTTP 401，实际 {resp.status_code}｜body={resp.body}"
    assert_biz_code(resp, expect_code)


def assert_paged(resp:ApiResponse) -> list[Any]:
    """断言分页结构 {total, list}，返回 list"""
    data = assert_success(resp)
    assert isinstance(data, dict), f"分页响应 data 应为 dict， 实际{type(data)}"
    assert "total" in data and "list" in data, f"分页响应缺少 total/list: data"
    assert isinstance(data["list"], list), f"list 应为数组: {data}"
    return data["list"]


def assert_field(data: dict, field: str, expect: Any) -> None:
    """断言字段值相等（含 None 安全检查）"""
    assert field in data, f"响应缺少字段 {field}｜data={data}"
    assert data[field] == expect, f"字段 {field} 期望 {expect!r}，实际 {data[field]!r}"


def assert_json_contains(data: dict, subset: dict) -> None:
    """断言 data 至少包含 subset 里的键值对（不必全等，适合字段多的 VO）"""
    for k, v in subset.items():
        assert k in data, f"缺少字段 {k} | data={data}"
        assert data[k] == v, f"字段 {k} 期望 {v!r}, 实际 {data[k]!r}"
