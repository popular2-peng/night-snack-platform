<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="layout-aside">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header height="60px" class="layout-header">
        <Header />
      </el-header>
      <el-main class="page-container">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background: var(--sidebar-bg);
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.18);
  z-index: 20;
  overflow: hidden;
}
.layout-header {
  padding: 0;
}
</style>
