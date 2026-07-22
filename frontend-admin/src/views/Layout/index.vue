<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
</script>

<template>
  <el-container style="height: 100vh">
    <el-aside width="220px">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header height="60px" style="padding: 0">
        <Header />
      </el-header>
      <el-main class="page-container">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
</style>
