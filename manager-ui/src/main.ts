import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { ensureApiBase } from './api/resolveApiBase'
import './assets/main.css'

void ensureApiBase().finally(() => {
  createApp(App).use(router).mount('#app')
})
