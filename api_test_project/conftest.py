"""全局夹具：配置 + 三端客户端"""
import pytest

from common.config_loader import load_config
from common.requests_client import ApiClient


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
