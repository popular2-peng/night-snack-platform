"""全局夹具：配置 + 三端客户端"""
import pytest

from common.config_loader import load_config
from common.requests_client import ApiClient
from common.data_loader import load_yaml
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
def upload_api(anon_client):
    return UploadApi(anon_client)


@pytest.fixture
def pay_api(anon_client):
    return PayApi(anon_client)


# ── 按角色名取接口对象（越权矩阵这类批量用例用）──
@pytest.fixture
def apis(user_api, merchant_api, admin_api):
    return {"user": user_api, "merchant": merchant_api, "admin": admin_api}

