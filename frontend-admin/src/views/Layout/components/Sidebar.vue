<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { Monitor, User, Shop, DocumentChecked, CollectionTag } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard',    title: '首页统计', icon: Monitor },
  { path: '/users',        title: '用户管理', icon: User },
  { path: '/merchants',    title: '商家管理', icon: Shop },
  { path: '/applications', title: '入驻审核', icon: DocumentChecked },
  { path: '/categories',   title: '品类管理', icon: CollectionTag },
]

const handleSelect = (path: string) => {
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
  height: calc(100% - 68px - 36px);
  border-right: none !important;
  padding: 8px 10px;
}

.el-menu .nav-item {
  margin: 2px 0;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  transition: all 0.2s ease;
  position: relative;
}
.el-menu .nav-item:hover {
  background-color: var(--sidebar-hover) !important;
  color: #fff !important;
  padding-left: 22px !important;
}
.el-menu .nav-item.is-active {
  background: linear-gradient(135deg, rgba(91, 141, 239, 0.18), rgba(91, 141, 239, 0.06)) !important;
  color: #fff !important;
  font-weight: 600;
}
.el-menu .nav-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  background: var(--sidebar-active);
  border-radius: 0 3px 3px 0;
}

.sidebar-footer {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  user-select: none;
  letter-spacing: 1px;
}
</style>
