"""业务接口层：URL 与参数拼装的唯一出处"""
from api.admin_api import AdminApi
from api.auth_api import AuthApi
from api.base_api import BaseApi
from api.merchant_api import MerchantApi
from api.pay_api import PayApi
from api.upload_api import UploadApi
from api.user_api import UserApi

__all__ = ["BaseApi", "AuthApi", "UserApi", "MerchantApi",
           "AdminApi", "UploadApi", "PayApi"]