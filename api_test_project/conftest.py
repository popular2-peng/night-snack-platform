"""全局夹具：配置 + 三端客户端"""
import pytest
import copy

from common.config_loader import load_config
from common.requests_client import ApiClient
from common.data_loader import load_yaml
from common.assertions import assert_success
from utils import da_helper
from utils.data_generator import gen_client_token
from api import AdminApi, AuthApi, MerchantApi, PayApi, UploadApi, UserApi


@pytest.fixture(scope="session")
def env_config():
    return load_config()


@pytest.fixture(scope="session")
def base_url(env_config):
    """被测服务根地址（不含 /api）"""
    return env_config["server"]["base_url"]


@pytest.fixture(scope="session")
def timeout(env_config):
    return env_config["server"]["timeout"]


# ── 三端 token（session 级：整个会话只登录一次，省时间）──
@pytest.fixture(scope="session")
def user_token(base_url, timeout, env_config):
    """用户 token：用配置里的主测试账号登录一次"""
    acct = env_config["accounts"]["user"]
    client = ApiClient(base_url, timeout=timeout)
    resp = client.post("/api/user/login",
                       json_body={"phone": acct["phone"], "password": acct["password"]})
    assert resp.code == 0, f"用户登录失败: {resp.body}"
    return resp.data["accessToken"]


@pytest.fixture(scope="session")
def merchant_token(base_url, timeout, env_config):
    acct = env_config["accounts"]["merchant"]
    client = ApiClient(base_url, timeout=timeout)
    resp = client.post("/api/merchant/login",
                       json_body={"phone": acct["phone"], "password": acct["password"]})
    assert resp.code == 0, f"商家登录失败: {resp.body}"
    return resp.data["accessToken"]


@pytest.fixture(scope="session")
def admin_token(base_url, timeout, env_config):
    acct = env_config["accounts"]["admin"]
    client = ApiClient(base_url, timeout=timeout)
    resp = client.post("/api/admin/login",
                       json_body={"username": acct["username"], "password": acct["password"]})
    assert resp.code == 0, f"管理员登录失败: {resp.body}"
    return resp.data["accessToken"]


# ── 三端客户端（function 级：每条用例干净实例，避免用例间状态串味）──
@pytest.fixture
def user_client(base_url, timeout, user_token):
    """用户端客户端（已带用户 token）"""
    return ApiClient(base_url, token=user_token, timeout=timeout)


@pytest.fixture
def merchant_client(base_url, timeout, merchant_token):
    return ApiClient(base_url, token=merchant_token, timeout=timeout)


@pytest.fixture
def admin_client(base_url, timeout, admin_token):
    return ApiClient(base_url, token=admin_token, timeout=timeout)


@pytest.fixture
def anon_client(base_url, timeout):
    """匿名客户端（不带 token，测未登录/公开接口用）"""
    return ApiClient(base_url, timeout=timeout)


@pytest.fixture(scope="session", autouse=True)
def _close_db_after_all():
    """会话结束后关闭数据库连接"""
    yield
    from utils import da_helper
    da_helper.close()


@pytest.fixture(scope="session", autouse=True)
def _close_redis_after_all():
    """会话结束后关闭redis数据库连接"""
    yield
    from utils import redis_helper
    redis_helper.close()


@pytest.fixture(scope="session")
def yaml_data():
    """通用取数工厂：用例里 yaml_data("login") 拿到整个文件（dict）"""
    def _load(name: str):
        return load_yaml(name)

    return _load


@pytest.fixture(scope="session")
def seed_data(yaml_data):
    """种子数据锚点：商家 / 商品 / 规格 / 地址的固定 id 与固定属性"""
    return yaml_data("seed")


@pytest.fixture(scope="session")
def login_data(yaml_data):
    """登录相关数据：成功账号引用 + 失败矩阵"""
    return yaml_data("login")


# ── 业务接口对象（function 级：与 client 同生命周期，用例间不串状态）──
@pytest.fixture
def user_api(user_client):
    """已登录的用户端接口"""
    return UserApi(user_client)


@pytest.fixture
def anon_api(anon_client):
    """匿名视角（登录 / 注册 / 浏览搜索这类接口用）"""
    return UserApi(anon_client)


@pytest.fixture
def merchant_api(merchant_client):
    return MerchantApi(merchant_client)


@pytest.fixture
def admin_api(admin_client):
    return AdminApi(admin_client)


@pytest.fixture
def auth_api(anon_client):
    return AuthApi(anon_client)


@pytest.fixture
def anon_apis(base_url, timeout):
    """
    匿名视角的三端接口对象 —— 登录类用例用

    ⚠️ 三个 API 类**各有自己的 login()**，端点不同：
       UserApi.login()     → POST /api/user/login      body {phone, password}
       MerchantApi.login() → POST /api/merchant/login  body {phone, password}
       AdminApi.login()    → POST /api/admin/login     body {username, password}
    """
    def _new(cls):
        return cls(ApiClient(base_url, timeout=timeout))     # 不带 token 的 client
    return {
        "user": _new(UserApi),
        "merchant": _new(MerchantApi),
        "admin": _new(AdminApi)
    }


@pytest.fixture
def upload_api(anon_client):
    return UploadApi(anon_client)


@pytest.fixture
def pay_api(anon_client):
    return PayApi(anon_client)


# ── 按角色名取接口对象（越权矩阵这类批量用例用）──
@pytest.fixture
def apis(user_api, merchant_api, admin_api):
    return {"user": user_api, "merchant": merchant_api, "admin": admin_api}


# ============================================================
#  场景夹具层
#  作用：把 data/*.yaml 里的「key」解析成真实 id；
#        把 prepare: xxx 声明的前置条件实现成可复用函数
# ============================================================
@pytest.fixture
def order_payload(seed_data, yaml_data):
    """
    把 data/order.yaml 里的 case 拼成可直接发请求的下单 payload。

    解析规则：
      · case 里给 key（merchant_key / address_key / product_key / spec_key）→ 查 seed 解析成真实 id
      · case 里直接给 id（merchant_id / address_id / product_id / spec_id）→ 原样使用
      · case 里没给的字段 → 用 base（默认取 create_success）补齐，
        这样「只改一个字段」的负向用例不用把整个 payload 抄一遍
      · clientToken 永远现生成（写死会命中幂等，用例假绿）
      · 想删掉某个字段（测必填校验）→ 传 drop=["merchantId"]
    """
    base = yaml_data("order")["create_success"]

    def _item(src: dict) -> dict:
        item: dict = {}
        if "product_key" in src:
            item["productId"] = seed_data["products"][src["product_key"]]["id"]
        elif "product_id" in src:
            item["productId"] = src["product_id"]

        if "spec_key" in src:
            product = seed_data["products"][src["product_key"]]
            item["specId"] = product["specs"][src["spec_key"]]["id"]
        elif "spec_id" in src:
            item["specId"] = src["spec_id"]

        if "quantity" in src:
            item["quantity"] = src["quantity"]
        return item

    def _build(case: dict, drop: tuple = (), **overrides) -> dict:
        merged = {**base, **case}          # 用 base 补齐缺失字段
        payload: dict = {}

        if "merchant_key" in merged:
            payload["merchantId"] = seed_data["merchants"][merged["merchant_key"]]["id"]
        elif "merchant_id" in merged:
            payload["merchantId"] = merged["merchant_id"]

        if "address_key" in merged:
            payload["addressId"] = seed_data["addresses"][merged["address_key"]]["id"]
        elif "address_id" in merged:
            payload["addressId"] = merged["address_id"]

        # items 的三种写法：显式 items / 顶层单个商品 / 回落到 base
        if "items" in case:
            items_src = case["items"]
        elif "product_key" in case or "product_id" in case:
            items_src = [case]
        else:
            items_src = merged["items"]
        payload["items"] = [_item(i) for i in items_src]

        payload["payChannel"] = merged.get("pay_channel", "WECHAT_MOCK")

        # client_token: '' 表示「故意不传」（测必填），其余情况都现生成
        if merged.get("client_token", "auto") != "":
            payload["clientToken"] = gen_client_token()

        for key in drop:
            payload.pop(key, None)
        payload.update(overrides)
        return payload

    return _build


@pytest.fixture
def scenes(user_api, merchant_api, yaml_data, order_payload):
    """
    按 data 里 `prepare:` 的名字造前置数据。

    用例里这样用：
        order = scenes["create_and_pay_order"]()
        # 或者跟着数据文件走：
        order = scenes[case["prepare"]]()
    """
    base_case = yaml_data("order")["create_success"]

    def create_pending_order() -> dict:
        """待支付订单（PENDING_PAY）"""
        return assert_success(user_api.create_order(order_payload(base_case)))

    def create_and_pay_order() -> dict:
        """已支付订单（PAID）—— 微信 mock 渠道，无外部依赖"""
        order = create_pending_order()
        assert_success(user_api.pay(order["orderNo"], "WECHAT_MOCK"))
        return order

    def create_and_cancel_order() -> dict:
        """已取消订单（CANCELLED）"""
        order = create_pending_order()
        assert_success(user_api.cancel_order(order["id"]))
        return order

    def create_completed_order() -> dict:
        """已完成订单（COMPLETED）—— 走完支付→接单→制作→完成"""
        order = create_and_pay_order()
        assert_success(merchant_api.accept_order(order["id"]))
        assert_success(merchant_api.cooking_order(order["id"]))
        assert_success(merchant_api.complete_order(order["id"]))
        return order

    def merchant_approval_without_approve() -> dict:
        """
        已申请退款、等商家审批的订单（REFUNDING）

        ⭐ 关键技巧：把 pay_time 改成 5 分钟前，绕过「支付 1 分钟内即时退款」的分支，
           否则会直接退成 REFUNDED，压根走不到商家审批
        """
        order = create_and_pay_order()
        da_helper.execute(
            "UPDATE `order` SET pay_time = NOW() - INTERVAL 5 MINUTE WHERE order_no = %s",
            (order["orderNo"],))
        assert_success(user_api.apply_order(order["id"]))
        return order

    return {
        "create_pending_order": create_pending_order,
        "create_and_pay_order": create_and_pay_order,
        "create_and_cancel_order": create_and_cancel_order,
        "create_completed_order": create_completed_order,
        "merchant_approval_without_approve": merchant_approval_without_approve,
    }


@pytest.fixture
def clean_cart(user_api):
    """
    购物车用例专用：用例前后各清一次

    购物车是 Redis 里的共享状态（key = cart:{user_id}），
    不清的话上一条用例加的商品会串到下一条，断言数量时对不上。
    """
    user_api.clear_cart()
    yield
    user_api.clear_cart()


@pytest.fixture
def api_factory(base_url, timeout):
    """按手机号临时登录，返回该账号的 UserApi —— 跨用户越权用例用"""
    def _make(phone: str, password: str) -> UserApi:
        client = ApiClient(base_url, timeout=timeout)
        data = assert_success(client.post("/api/user/login",
                                         json_body={"phone": phone, "password": password}))
        return UserApi(ApiClient(base_url, token=data["accessToken"], timeout=timeout))
    return _make


