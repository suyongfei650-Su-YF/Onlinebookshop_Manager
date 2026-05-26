<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import PortalSiteHeader from './views/portal/components/PortalSiteHeader.vue'
import PortalAiAssistant from './views/portal/components/PortalAiAssistant.vue'

const route = useRoute()
const showPortalChrome = computed(() => route.path === '/portal' || route.path.startsWith('/portal/'))
</script>

<template>
  <PortalSiteHeader v-if="showPortalChrome" />
  <RouterView />
  <PortalAiAssistant v-if="showPortalChrome" />
</template>
