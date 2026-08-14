<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { watch } from 'vue'

const route = useRoute()
const router = useRouter()

const active = ref(route.path)

watch(() => route.path, (p) => { active.value = p })

const tabs = [
  { path: '/home',    title: '首页', icon: 'home-o' },
  { path: '/orders',  title: '订单', icon: 'orders-o' },
  { path: '/profile', title: '我的', icon: 'user-o' },
]

const onTabChange = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="layout">
    <main class="layout-content" :class="{ 'no-tabbar': !route.meta.tabbar }">
      <router-view />
    </main>
    <!-- 仅 tab 页显示底部导航（二级页如收货地址不显示） -->
    <van-tabbar v-if="route.meta.tabbar" :model-value="active" @change="onTabChange" fixed placeholder safe-area-inset-bottom>
      <van-tabbar-item v-for="t in tabs" :key="t.path" :name="t.path" :icon="t.icon">
        {{ t.title }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.layout-content {
  flex: 1;
  padding-bottom: 56px;
}
.layout-content.no-tabbar {
  padding-bottom: 0;
}
</style>
