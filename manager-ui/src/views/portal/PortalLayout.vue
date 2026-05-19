<template>
  <div class="font-body-md bg-background min-h-screen">
    <TopNav />
    <main class="pt-16">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import TopNav from '../user/components/UserTopNav.vue'
</script>

