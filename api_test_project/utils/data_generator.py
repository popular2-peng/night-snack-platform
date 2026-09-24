"""
测试数据生成器 —— 保证数据唯一性，让用例可无限次重复执行

设计原则：
  1. 默认只用标准库（random / time / uuid / datetime），零额外依赖
  2. 所有生成器的返回值都满足被测系统的校验规则（如手机号 1[3-9] 开头 11 位）
  3. 生成的数据「可识别」：前缀固定，便于排查与清理
"""
from __future__ import annotations

import random
import time
import string
import uuid
from datetime import datetime

# ── 可识别前缀（便于在库里区分"自动化造的"数据）──
TEST_PREFIX = "自动测试"


def gen_phone() -> str:
    """
    生成唯一手机号（11 位，1[3-9] 开头）。

    实现：'139' + 当前毫秒时间戳的后 5 位 + 3 位随机数 = 11 位。
          时间戳保证跨时间不重复，3 位随机（1000 种）防止同毫秒批量造数时碰撞。
    注意：后端校验是 `^1[3-9]\\d{9}$`（恰好 11 位，见 RegisterDTO/AddressDTO/
          MerchantRegisterDTO），多一位都会返回 1100，所以位数不能随意加。
    """
    tail = f"{int(time.time() * 1000) % 100_000:05d}{random.randint(0, 999):03d}"
    return f"139{tail}"


def gen_username(prefix: str = TEST_PREFIX) -> str:
    """生成带前缀的随机名称（注册用户昵称、商品名、品类名等）"""
    return f"{prefix}{datetime.now():%m%d%H%M%S}{random.randint(100, 999)}"


def gen_client_token() -> str:
    """生成下单幂等键（uuid4 去掉横线，32 位，长度 ≤64 满足后端约束）"""
    return f"at-{uuid.uuid4().hex}"          # at- 表示 auto test，便于识别


def gen_biz_no(prefix:str = "AT") -> str:
    """生成业务号（如测试用的订单备注号、外部单号）"""
    return f"{prefix}{datetime.now():%Y%m%d%H%M%S}{random.randint(1000, 9999)}"


def gen_password(length: int = 8) -> str:
    """
    生成符合后端密码规则的随机密码（字母 + 数字组合）。

    后端规则（见 CLAUDE.md / api.md）：6~20 位，必须含字母和数字。
    """
    letters = random.choices(string.ascii_lowercase, k=max(2, length // 2))
    digits = random.choices(string.digits, k=max(2, length - len(letters)))
    pwd = letters + digits
    random.shuffle(pwd)
    return ''.join(pwd)


def gen_address_detail() -> str:
    """生成带可识别前缀的地址详情"""
    return f"{TEST_PREFIX}地址-{uuid.uuid4().hex[:8]}"


def gen_random_string(length: int = 10) -> str:
    """通用随机字符串（测试超长/特殊字符场景）"""
    return "".join(random.choices(string.ascii_letters + string.digits, k=length))


# ── 可选：Faker 版本（更真实的中文姓名/地址，装了 faker 再用）──
def gen_realistic_name() -> str:
    """用 Faker 生成中文姓名（需 pip install faker；未安装则回退到标准库）"""
    try:
        from faker import Faker
    except ImportError:
        return gen_username()
    return Faker("zh_CN").name()


def gen_realistic_address() -> dict[str, float | str]:
    """用 Faker 生成中文地址 + 北京范围内的经纬度（需 faker）"""
    detail = gen_address_detail()
    try:
        from faker import Faker
        detail = f"{TEST_PREFIX}-" + Faker("zh_CN").address().replace("\n", "")
    except ImportError:
        pass
    # 北京五环内随机坐标（配送范围测试用；后端会自行校验范围）
    return {
        "detail": detail,
        "lat": round(random.uniform(39.85, 40.05), 6),
        "lng": round(random.uniform(116.30, 116.55), 6),
    }


if __name__ == '__main__':
    # 自测：python -m utils.data_generator
    print("手机号      :", gen_phone())
    print("手机号(再次):", gen_phone(), "  ← 应不同")
    print("用户名      :", gen_username())
    print("幂等键      :", gen_client_token())
    print("密码        :", gen_password())
    print("地址详情    :", gen_address_detail())

