<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { Fold, Expand } from '@element-plus/icons-vue'
import Sidebar from './components/Sidebar.vue'

const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)

const handleLogout = async () => {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <Sidebar :collapsed="collapsed" />
    <div class="layout-main">
      <header class="layout-header">
        <el-icon class="collapse-btn" @click="collapsed = !collapsed">
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
        <span class="header-title">商家工作台</span>
        <span style="flex: 1" />
        <el-dropdown trigger="click">
          <span class="user-chip">
            <el-avatar :size="30" style="background: var(--primary)">{{ authStore.username?.charAt(0)?.toUpperCase() }}</el-avatar>
            <span class="user-name">{{ authStore.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/shop')">店铺设置</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.layout-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
  gap: 12px;
}
.collapse-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: color 0.15s;
}
.collapse-btn:hover { color: var(--primary); }
.header-title { font-size: 15px; font-weight: 600; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}
.user-chip:hover { background: var(--bg); }
.user-name { font-size: 14px; }
.layout-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: var(--bg);
}
</style>
