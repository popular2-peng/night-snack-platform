"""
YAML 测试数据加载器

职责：
  1. 定位 data/ 目录，读取 data/*.yaml
  2. 进程内缓存：同一文件无论被引用多少次，只读一次磁盘
  3. 支持 ${ENV_VAR} 占位符（复用 config_loader 的能力）
  4. 找不到文件时打印「可用文件清单」，避免拼错名字干瞪眼

用法：
    from common.data_loader import load_yaml, get

    login = load_yaml("login")                       # 整个文件
    code = get("login", "failed_list.0.expect_code") # 按路径取

自测（顺带校验所有 yaml 语法）：
    python -m common.data_loader
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml

from common.config_loader import PROJECT_ROOT, resolve_placeholders

DATA_DIR = PROJECT_ROOT / "data"

# 进程内缓存：key = 规范化后的文件标识（不含后缀）
_CACHE: dict[str, Any] = {}


def data_files() -> list[Path]:
    """返回 data/ 下所有 yaml 文件（已排序，便于排查与自测）"""
    if not DATA_DIR.exists():
        return []
    return sorted(DATA_DIR.rglob("*.yaml")) + sorted(DATA_DIR.rglob("*.yml"))


def _normalize(name: str) -> str:
    rel = name.replace("\\", "/").strip("/")
    if rel.endswith((".yaml", ".yml")):
        rel = rel.rsplit(".", 1)[0]
    return rel


def _key_to_path(rel: str) -> Path:
    """
    把用例里写的名字解析成真实文件路径。

    支持三种写法：
        load_yaml("login")         -> data/login.yaml
        load_yaml("login.yaml")    -> data/login.yaml
        load_yaml("order/refund")  -> data/order/refund.yaml（数据多了再拆子目录）
    """
    for suffix in (".yaml", ".yml"):
        path = DATA_DIR / f"{rel}{suffix}"
        if path.exists():
            return path

    available = "、".join(p.stem for p in data_files()) or "（data/ 目录为空或不存在）"
    raise FileNotFoundError(
        f"测试数据文件不存在：{DATA_DIR / (rel + '.yaml')}\n"
        f"  可用文件：{available}\n"
        f"  提示：请确认文件放在 {DATA_DIR} 下，且名字与 load_yaml() 的入参一致"
    )


def load_yaml(name: str, *, reload: bool = False) -> Any:
    """
    读取并缓存一个 YAML 测试数据文件。

    :param name:   文件标识，如 "login" / "login.yaml" / "order/refund"
    :param reload: True 时强制重新读磁盘（本地改完 yaml 又不想重启 pytest 时用）
    :return:       解析后的 Python 对象（dict / list）

    ⚠️ 返回的是缓存对象本身（同一个 dict），用例里请**只读不写**。
       需要改动（比如往 payload 里塞动态生成的 clientToken）请先 copy.deepcopy()。
    """
    rel = _normalize(name)

    if rel in _CACHE and not reload:
        return _CACHE[rel]

    path = _key_to_path(rel)
    with open(path, encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    if raw is None:
        raw = {}
    if isinstance(raw, str):
        raise ValueError(
            f"测试数据文件 {path.name} 顶层必须是 dict 或 list，"
            f"当前解析成了字符串 —— 请检查缩进，或是否漏了 key 后面的冒号"
        )

    data = resolve_placeholders(raw)          # 数据文件里也能写 ${XXX}
    _CACHE[rel] = data
    return data


def get(name: str, key_path: str, default: Any = None) -> Any:
    """
    按 "a.b.c" 路径取数据，取不到返回 default。

        get("login", "failed_list.0.expect_code")   # -> 2004
        get("seed", "merchants.night_bbq.id")       # -> 1
    """
    node = load_yaml(name)
    for part in key_path.split("."):
        if isinstance(node, list):
            try:
                node = node[int(part)]
            except (ValueError, IndexError):
                return default
        elif isinstance(node, dict):
            if part not in node:
                return default
            node = node[part]
        else:
            return default
    return node


def clear_cache() -> None:
    """清空缓存（调试或需要强制重读全部数据时用）"""
    _CACHE.clear()


if __name__ == "__main__":
    # 自测 + 语法体检：python -m common.data_loader
    files = data_files()
    if not files:
        print(f"[data] data/ 目录为空或不存在：{DATA_DIR}")
        raise SystemExit(1)

    print(f"[data] 目录：{DATA_DIR}")
    print(f"[data] 共 {len(files)} 个数据文件\n")

    bad = 0
    for p in files:
        rel = p.relative_to(DATA_DIR).with_suffix("").as_posix()
        try:
            data = load_yaml(rel)
            shape = (f"dict({len(data)} keys)" if isinstance(data, dict)
                     else f"list({len(data)} items)")
            print(f"  [OK]   {rel:<26} {shape}")
        except Exception as e:               # 自测脚本要一次打印全部问题，不提前退出
            bad += 1
            print(f"  [FAIL] {rel:<26} {e}")

    print(f"\n[data] 校验完成：{len(files) - bad} 个正常，{bad} 个异常")
    raise SystemExit(1 if bad else 0)