<template>
  <nav class="fixed top-0 w-full z-50 flex justify-between items-center px-16 bg-stone-900 h-16 border-b border-stone-800">
    <RouterLink class="text-2xl font-bold text-white tracking-widest" to="/portal">馆藏书屋</RouterLink>
    <div class="flex items-center gap-8 text-stone-300">
      <RouterLink class="hover:text-white" to="/portal/cart">购物车</RouterLink>
      <RouterLink class="hover:text-white" to="/portal/favorites">收藏</RouterLink>
      <RouterLink class="hover:text-white" to="/portal/profile">个人中心</RouterLink>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
