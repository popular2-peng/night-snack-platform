"""
    注册/登录/刷新/权限
"""
import pytest

from api import UserApi
from common.assertions import assert_success, assert_biz_code, assert_unauthorized
from common.data_loader import load_yaml
from common.requests_client import ApiClient
from utils import da_helper
from utils.data_generator import gen_phone


CASES = load_yaml("login")["failed_list"]
TYPE_CASES = [c for c in load_yaml("login")["type_cases"] if c["name"] != "phone_array"]
M_CASES = load_yaml("login")["merchant_failed_list"]
A_CASES = load_yaml("login")["admin_failed_list"]
BAD_PHONES = load_yaml("login")["register"]["bad_phone_list"]
ROLE_CASES = load_yaml("authz")["role_mismatch"]
NO_TOKEN_CASES = load_yaml("authz")["no_token"]


@pytest.mark.smoke
@pytest.mark.auth
@pytest.mark.parametrize("role", ["user", "merchant", "admin"])
def test_login_success(role, anon_apis, env_config, login_data):
    """三端登录成功：返回双令牌 + 角色正确"""
    acct = env_config["accounts"][login_data["success"][role]["account_key"]]
    expect_role = login_data["success"][role]["expect_role"]

    # user / merchant 用 phone 登录，admin 用 username —— 两者取其一
    identity = acct.get("phone") or acct.get("username")
    data = assert_success(anon_apis[role].login(identity, acct["password"]))

    assert data["role"] == expect_role, f"角色期望 {expect_role}，实际 {data['role']}"
    assert data["accessToken"], "accessToken 为空"
    assert data["refreshToken"], "refreshToken 为空"


@pytest.mark.p1
@pytest.mark.auth
@pytest.mark.parametrize("case", CASES, ids=[c["name"] for c in CASES])
def test_user_login_failed(anon_api, case):
    resp = anon_api.login(case["phone"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg=f"[{case['title']}]")


@pytest.mark.p2
@pytest.mark.auth
@pytest.mark.parametrize("case", TYPE_CASES, ids=[c["name"] for c in TYPE_CASES])
def test_login_phone_type(anon_api, case):
    """
    手机号类型容错

    ⚠️ Jackson 会把 JSON 标量强转成 String，「传数字」不会报 1100：
       整数 → "13800000003"（查得到 → 登录成功）；浮点 → "1.38E10"（查不到 → 2003）
    """
    resp = anon_api.login(case["phone"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg=f"[{case['title']}]")


@pytest.mark.xfail(
    reason="已知缺陷：入参类型不匹配返回 1999 系统繁忙，应返回 1100 参数校验失败",
    strict=True
)
@pytest.mark.p2
@pytest.mark.auth
def test_login_phone_wrong_type_should_be_param_error(anon_api, yaml_data):
    """
    期望 1100，实测 1999 —— 真缺陷，不是用例写错

    开发修好后（异常处理器把反序列化异常转 1100）这条会变成 XPASS，
    strict=True 会提醒你去掉 xfail 标记。
    """
    case = next(c for c in yaml_data("login")["type_cases"] if c["name"] == "phone_array")
    resp = anon_api.login(case["phone"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg="[类型不匹配应为参数错误]")


@pytest.mark.p1
@pytest.mark.auth
@pytest.mark.parametrize("case", M_CASES, ids=[c["name"] for c in M_CASES])
def test_merchant_login_failed(anon_apis, case):
    resp = anon_apis["merchant"].login(case["phone"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg=f"{[case['title']]}")


@pytest.mark.p1
@pytest.mark.auth
@pytest.mark.parametrize("case", A_CASES, ids=[c["name"] for c in A_CASES])
def test_admin_login_failed(anon_apis, case):
    resp = anon_apis["admin"].login(case["username"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg=f"{[case['title']]}")


@pytest.mark.p1
@pytest.mark.auth
def test_register_success(anon_api, yaml_data):
    """注册即登录：返回双令牌 + username 按规则生成"""
    case = yaml_data("login")["register"]["success"]
    phone = gen_phone()
    try:
        data = assert_success(anon_api.register(phone, case["password"]))

        assert data["role"] == case["expect_role"]
        username = data["username"]
        assert username.startswith(case["expect_username_prefix"]), \
            f"username 应以「{case['expect_username_prefix']}」开头，实际 {username}"
        assert len(username) == case["expect_username_length"], \
            f"username 长度应为 {case['expect_username_length']}，实际 {len(username)}"

        # 拿注册返回的 token 调需要鉴权的接口，验证 token 真的可用
        authed = UserApi(ApiClient(anon_api.client.base_url, token=data["accessToken"],
                                   timeout=anon_api.client.timeout))
        assert_success(authed.info())
    finally:
        # 造出来的用户用完软删，避免污染用户列表
        da_helper.execute("UPDATE `user` SET deleted = 1 WHERE phone = %s", (phone,))


@pytest.mark.p1
@pytest.mark.auth
def test_register_duplicate_phone(anon_api, yaml_data):
    case = yaml_data("login")["register"]["duplicate_phone"]
    resp = anon_api.register(case["phone"], case["password"])
    assert_biz_code(resp, case["expect_code"], msg="手机号已注册")


@pytest.mark.p2
@pytest.mark.auth
@pytest.mark.parametrize("case", BAD_PHONES, ids=[c["name"] for c in BAD_PHONES])
def test_register_bad_phone(anon_api, case):
    resp = anon_api.register(case["phone"], "test123")
    assert_biz_code(resp, case["expect_code"], msg=f"[非法手机号-{case['name']}]")


@pytest.mark.p1
@pytest.mark.auth
def test_refresh_success(anon_api, auth_api, env_config):
    """正常刷新：换到的新 accessToken 应当可用"""
    acct = env_config["accounts"]["user"]
    login = assert_success(anon_api.login(acct["phone"], acct["password"]))

    new = assert_success(auth_api.refresh(login["refreshToken"]))
    assert new["accessToken"] and new["refreshToken"]

    authed = UserApi(ApiClient(anon_api.client.base_url, token=new["accessToken"],
                               timeout=anon_api.client.timeout))
    assert_success(authed.info())


@pytest.mark.p1
@pytest.mark.auth
def test_refresh_with_access_token_rejected(anon_api, auth_api, env_config):
    """用 accessToken 冒充 refreshToken → 必须被拒（1004）"""
    acct = env_config["accounts"]["user"]
    login = assert_success(anon_api.login(acct["phone"], acct["password"]))

    resp = auth_api.refresh(login["accessToken"])
    assert_biz_code(resp, 1004, msg="[accessToken 冒充 refreshToken]")


@pytest.mark.p2
@pytest.mark.auth
@pytest.mark.parametrize("case", load_yaml("login")["refresh"]["bad_list"],
                         ids=[c["name"] for c in load_yaml("login")["refresh"]["bad_list"]])
def test_refresh_bad(auth_api, case, env_config):
    token = case["refresh_token"]
    if token == "${ACCESS_TOKEN}":
        acct = env_config["accounts"]["user"]
        token = assert_success(
            ApiClient(auth_api.client.base_url, timeout=auth_api.client.timeout)
            .post("/api/user/login", json_body={"phone": acct["phone"], "password": acct["password"]})
        )["accessToken"]
    resp = auth_api.refresh(token)
    assert_biz_code(resp, case["expect_code"], msg=f"[{case['name']}]")


@pytest.mark.p1
@pytest.mark.auth
@pytest.mark.parametrize("case", ROLE_CASES, ids=[c["name"] for c in ROLE_CASES])
def test_role_mismatch(apis, case):
    """USER 调商家/管理接口、MERCHANT 调用户/管理接口 → 一律 1002"""
    api = apis[case["actor"]]
    resp = api.request(case["method"], case["path"])
    assert_biz_code(resp, case["expect_code"], msg=f"[{case['name']}]")


@pytest.mark.p1
@pytest.mark.auth
@pytest.mark.parametrize("case", NO_TOKEN_CASES, ids=[c["name"] for c in NO_TOKEN_CASES])
def test_no_token_rejected(anon_api, case):
    """不带 token 访问受保护接口 → 1001"""
    resp = anon_api.request(case["method"], case["path"])
    assert_unauthorized(resp, case["expect_code"])


@pytest.mark.p1
@pytest.mark.auth
def test_user_read_other_user_order(api_factory, user_api, order_payload,
                                    yaml_data, seed_data):
    """读别人的订单 → 4001"""
    u2 = seed_data["users"]["normal_2"]
    addr2 = seed_data["addresses"]["wangjing_garden"]
    other_api = api_factory(u2["phone"], u2["password"])

    # 用 conftest 里现成的 order_payload 拼一份最小可下单单据
    others_order = assert_success(
        other_api.create_order(
            order_payload(yaml_data("order")["create_success"], addressId=addr2["id"])))

    resp = user_api.get_order(others_order["id"])
    assert_biz_code(resp, 1002, msg="[读他人订单]")


@pytest.mark.p1
@pytest.mark.auth
def test_merchant_operate_other_shop_product(merchant_api, seed_data):
    """商家 1 改商家 2 的商品 → 3301"""
    target = seed_data["products"]["no_spec_dish"]     # 属商家 2
    resp = merchant_api.update_product(target["id"], {
        "name": "越权改名",
        "categoryId": 1,
        "price": 9.99
    })
    assert_biz_code(resp, 3301, msg="[跨店操作商品]")

