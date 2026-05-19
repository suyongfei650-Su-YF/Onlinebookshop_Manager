<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import PortalSiteHeader from './views/portal/components/PortalSiteHeader.vue'

const route = useRoute()
const showPortalHeader = computed(() => route.path === '/portal' || route.path.startsWith('/portal/'))
</script>

<template>
  <PortalSiteHeader v-if="showPortalHeader" />
  <RouterView />
</template>
