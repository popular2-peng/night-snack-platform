<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showConfirmDialog } from 'vant'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = async () => {
  try {
    await showConfirmDialog({ title: '提示', message: '确定退出登录吗？' })
  } catch {
    return
  }
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="profile-page">
    <!-- 用户信息 -->
    <div class="profile-header">
      <div class="avatar">{{ authStore.username?.charAt(0)?.toUpperCase() }}</div>
      <div class="info">
        <p class="name">{{ authStore.username }}</p>
        <p class="desc">普通用户</p>
      </div>
    </div>

    <van-cell-group inset style="margin-top: 16px">
      <van-cell title="收货地址" is-link to="/profile" />
      <van-cell title="我的收藏" is-link to="/profile" />
      <van-cell title="联系客服" is-link to="/profile" />
    </van-cell-group>

    <van-cell-group inset style="margin-top: 16px">
      <van-cell title="退出登录" is-link @click="handleLogout" />
    </van-cell-group>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 20px 0;
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0 20px;
  padding: 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
}
.info .name {
  font-size: 17px;
  font-weight: 600;
  margin: 0 0 2px;
}
.info .desc {
  font-size: 12px;
  color: #86909C;
  margin: 0;
}
</style>
