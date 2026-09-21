"""
    全局夹具：配置、日志、三端token
"""
import pytest

from common.config_loader import load_config


@pytest.fixture(scope='session')
def env_config():
    """会话级配置，整个测试过程只读一次配置"""
    return load_config()
