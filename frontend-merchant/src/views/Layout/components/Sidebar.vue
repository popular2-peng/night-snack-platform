<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { HomeFilled, List, Goods, Shop } from '@element-plus/icons-vue'

defineProps<{ collapsed?: boolean }>()

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard', title: '工作台', icon: HomeFilled },
  { path: '/orders',    title: '订单管理', icon: List },
  { path: '/products',  title: '商品管理', icon: Goods },
  { path: '/shop',      title: '店铺设置', icon: Shop },
]

const handleSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <span class="logo-icon">🍢</span>
      <span v-if="!collapsed" class="logo-text">
        <span class="logo-title">夜宵平台</span>
        <span class="logo-subtitle">MERCHANT</span>
      </span>
    </div>
    <el-menu
      :default-active="activeMenu"
      background-color="transparent"
      text-color="var(--sidebar-text)"
      active-text-color="#fff"
      @select="handleSelect"
      style="border-right: none"
      :collapse="collapsed"
      :collapse-transition="false"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" class="nav-item">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
    <div class="sidebar-footer">
      <span v-if="!collapsed">v1.0.0</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100%;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.sidebar.collapsed { width: 64px; }

.sidebar-logo {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  flex-shrink: 0;
}
.logo-icon {
  font-size: 26px;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  border-radius: 10px;
  flex-shrink: 0;
}
.logo-text { display: flex; flex-direction: column; line-height: 1.2; }
.logo-title { color: #fff; font-size: 16px; font-weight: 700; }
.logo-subtitle { color: rgba(255, 255, 255, 0.3); font-size: 10px; letter-spacing: 2px; }

.sidebar :deep(.el-menu) {
  flex: 1;
  padding: 8px 10px;
}
.sidebar :deep(.nav-item) {
  margin: 2px 0;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  transition: all 0.2s ease;
  position: relative;
}
.sidebar :deep(.nav-item:hover) {
  background-color: var(--sidebar-hover) !important;
  color: #fff !important;
}
.sidebar :deep(.nav-item.is-active) {
  background: linear-gradient(135deg, rgba(245, 154, 35, 0.2), rgba(245, 154, 35, 0.08)) !important;
  color: #fff !important;
  font-weight: 600;
}
.sidebar :deep(.nav-item.is-active::before) {
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
  flex-shrink: 0;
}
</style>
