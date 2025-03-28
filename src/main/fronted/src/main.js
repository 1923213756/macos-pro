import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// 全局错误处理
const app = createApp(App)

// 注册全局方法
app.config.globalProperties.$showMessage = function(message, type = 'info') {
    // 简易消息提示，实际项目中可以使用UI组件库提供的消息组件
    console.log(`[${type}] ${message}`);
    alert(message);
}

app.use(router)
app.use(store)
app.mount('#app')