<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showConfirmDialog } from 'vant'

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
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
    <!-- 未登录：提示登录，不展示个人信息 -->
    <div v-if="!authStore.isLoggedIn" class="guest-card">
      <div class="guest-avatar">🍜</div>
      <p class="guest-title">未登录</p>
      <p class="guest-desc">登录后可查看订单、管理收货地址</p>
      <van-button round type="primary" class="guest-btn" @click="router.push('/login')">
        去登录
      </van-button>
    </div>

    <template v-else>
      <!-- 用户信息 -->
      <div class="profile-header">
        <div class="avatar">{{ authStore.username?.charAt(0)?.toUpperCase() }}</div>
        <div class="info">
          <p class="name">{{ authStore.username }}</p>
          <p class="desc">普通用户</p>
        </div>
      </div>

      <van-cell-group inset style="margin-top: 16px">
        <van-cell title="收货地址" is-link to="/address" />
        <van-cell title="我的收藏" is-link to="/profile" />
        <van-cell title="联系客服" is-link to="/profile" />
      </van-cell-group>

      <van-cell-group inset style="margin-top: 16px">
        <van-cell title="退出登录" is-link @click="handleLogout" />
      </van-cell-group>
    </template>
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

/* ---- 未登录状态 ---- */
.guest-card {
  margin: 40px 20px 0;
  padding: 40px 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  text-align: center;
}
.guest-avatar {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: linear-gradient(135deg, #FFF3E0, #FFE0B2);
  border-radius: 50%;
}
.guest-title {
  font-size: 17px;
  font-weight: 600;
  margin: 0 0 6px;
}
.guest-desc {
  font-size: 13px;
  color: #86909C;
  margin: 0 0 20px;
}
.guest-btn {
  width: 160px;
}
</style>
