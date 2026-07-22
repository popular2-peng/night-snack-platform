<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { Monitor, User, Shop, DocumentChecked } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard',    title: '首页统计', icon: Monitor },
  { path: '/users',        title: '用户管理', icon: User },
  { path: '/merchants',    title: '商家管理', icon: Shop },
  { path: '/applications', title: '入驻审核', icon: DocumentChecked },
]

function handleSelect(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="sidebar-logo">
    <span class="logo-icon">🍜</span>
    <span class="logo-title">夜宵平台</span>
    <span class="logo-subtitle">ADMIN PANEL</span>
  </div>
  <el-menu
    :default-active="activeMenu"
    background-color="var(--sidebar-bg)"
    text-color="var(--sidebar-text)"
    active-text-color="var(--sidebar-active)"
    @select="handleSelect"
    style="border-right: none"
  >
    <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" class="nav-item">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
  <div class="sidebar-footer">
    <span>v1.0.0</span>
  </div>
</template>

<style scoped>
.el-menu {
  height: calc(100% - 64px - 32px);
  border-right: none !important;
}

.el-menu .nav-item {
  margin: 4px 8px;
  border-radius: 6px;
  height: 44px;
  line-height: 44px;
}
.el-menu .nav-item:hover {
  background-color: rgba(255, 255, 255, 0.06) !important;
}
.el-menu .nav-item.is-active {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.2), transparent) !important;
  border-left: 3px solid var(--sidebar-active);
}

.sidebar-footer {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.25);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  user-select: none;
}
</style>
