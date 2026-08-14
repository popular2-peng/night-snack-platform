<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, List, Goods, Shop } from '@element-plus/icons-vue'

defineProps<{ collapsed?: boolean }>()

const route = useRoute()

/** 当前高亮菜单项（el-menu router 模式按 index=path 匹配） */
const activeMenu = computed(() => route.path)

/** 订单管理默认展开（index 与 el-sub-menu 的 index 对应） */
const openedMenus = ['orders']
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
      class="sidebar-menu"
      :default-active="activeMenu"
      :default-openeds="openedMenus"
      :collapse="collapsed"
      :collapse-transition="false"
      background-color="transparent"
      text-color="var(--sidebar-text)"
      active-text-color="#fff"
      popper-class="sidebar-popper"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <template #title>工作台</template>
      </el-menu-item>

      <el-sub-menu index="orders">
        <template #title>
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </template>
        <el-menu-item index="/orders/today">今日订单</el-menu-item>
        <el-menu-item index="/orders/all">全部订单</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/products">
        <el-icon><Goods /></el-icon>
        <template #title>商品管理</template>
      </el-menu-item>

      <el-menu-item index="/shop">
        <el-icon><Shop /></el-icon>
        <template #title>店铺设置</template>
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
  flex-shrink: 0;
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

/* ---- Element Plus 菜单适配（只改颜色与圆角，不动定位/层级） ---- */
.sidebar-menu {
  flex: 1;
  padding: 8px 10px;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar-menu::-webkit-scrollbar { width: 4px; }
.sidebar-menu::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.12); border-radius: 2px; }

/* 一级/子级菜单项 */
.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}
.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: var(--sidebar-hover) !important;
  color: #fff !important;
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(245, 154, 35, 0.2), rgba(245, 154, 35, 0.08)) !important;
  color: #fff !important;
  font-weight: 600;
}

/* 子菜单标题 */
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--sidebar-hover) !important;
  color: #fff !important;
}

/* 内联展开的子菜单容器（跟随父项往下排，不浮动） */
.sidebar-menu :deep(.el-menu--inline) {
  background: transparent;
}
.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  height: 38px;
  line-height: 38px;
  font-size: 13px;
  padding-left: 46px !important;
  background: transparent;
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
