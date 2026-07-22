<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { Monitor, User, Shop, DocumentChecked } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard',   title: '首页统计', icon: Monitor },
  { path: '/users',       title: '用户管理', icon: User },
  { path: '/merchants',   title: '商家管理', icon: Shop },
  { path: '/applications', title: '入驻审核', icon: DocumentChecked },
]

function handleSelect(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="sidebar-logo">🍜 夜宵平台</div>
  <el-menu
    :default-active="activeMenu"
    background-color="#1a1a2e"
    text-color="#bfcbd9"
    active-text-color="#409EFF"
    @select="handleSelect"
    style="border-right: none"
  >
    <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.title }}</span>
    </el-menu-item>
  </el-menu>
</template>

<style scoped>
.el-menu {
  height: 100%;
}
</style>
