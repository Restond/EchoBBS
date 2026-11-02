import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 应用启动时尝试获取当前登录用户信息
const userStore = useUserStore()
userStore.fetchCurrentUser().finally(() => {
  app.mount('#app')
})
