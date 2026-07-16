import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './styles/base.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import { useMessageStore } from './stores/messages'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

const auth = useAuthStore()
const messages = useMessageStore()

setUnauthorizedHandler((redirectPath) => {
  messages.reset()
  auth.clearSession()
  if (router.currentRoute.value.name !== 'login') {
    void router.push({
      name: 'login',
      query: redirectPath ? { redirect: redirectPath } : undefined,
    })
  }
})

app.mount('#app')
