<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ArrowDown, SwitchButton, UserFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const titleMap: Record<string, string> = {
  '/dashboard': '数据概览',
  '/users': '用户管理',
  '/merchants': '商家管理',
  '/applications': '入驻审核',
}

const currentTitle = computed(() => titleMap[route.path] || route.meta?.title || '')

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="header-bar">
    <div class="header-left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-avatar :size="32" :icon="UserFilled" />
          <span class="username">{{ authStore.username }}</span>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.header-bar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 1px 0 var(--border-light);
  z-index: 10;
  position: relative;
}
.header-left {
  display: flex;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 14px 6px 6px;
  border-radius: 24px;
  transition: all var(--transition);
  background: var(--bg);
}
.user-info:hover {
  background: var(--primary-bg);
}
.username {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}
.arrow-icon {
  font-size: 12px;
  color: var(--text-secondary);
  transition: transform var(--transition);
}
.user-info:hover .arrow-icon {
  transform: rotate(180deg);
}
</style>
