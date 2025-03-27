<template>
  <header class="app-header">
    <div class="container">
      <div class="logo">
        <router-link to="/">
          <img src="@/assets/logo.png" alt="FoodMap" class="logo-image">
          <span class="logo-text">美食地图</span>
        </router-link>
      </div>

      <nav class="main-nav">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/about" class="nav-item">关于我们</router-link>
      </nav>

      <div class="user-actions">
        <template v-if="isLoggedIn">
          <router-link to="/shop/manage" class="action-btn manage-btn">商铺管理</router-link>
          <button @click="logout" class="action-btn logout-btn">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="action-btn login-btn">登录</router-link>
          <router-link to="/register" class="action-btn register-btn">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AppHeader',
  data() {
    return {
      isLoggedIn: false
    };
  },
  created() {
    // 检查登录状态
    this.checkLoginStatus();
  },
  methods: {
    checkLoginStatus() {
      // 实际项目中应该从vuex或localStorage检查登录状态
      const shop = localStorage.getItem('currentShop');
      this.isLoggedIn = !!shop;
    },
    logout() {
      // 清除登录信息并刷新页面
      localStorage.removeItem('currentShop');
      this.isLoggedIn = false;
      this.$router.push('/');
    }
  }
};
</script>

<style scoped>
.app-header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #333;
}

.logo-image {
  height: 40px;
  margin-right: 10px;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
}

.main-nav {
  display: flex;
  gap: 20px;
}

.nav-item {
  text-decoration: none;
  color: #333;
  font-size: 16px;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.nav-item:hover {
  background-color: #f5f5f5;
}

.user-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
}

.login-btn {
  background-color: transparent;
  color: #ff6600;
  border: 1px solid #ff6600;
}

.register-btn, .manage-btn {
  background-color: #ff6600;
  color: white;
  border: none;
}

.logout-btn {
  background-color: transparent;
  color: #666;
  border: 1px solid #ddd;
}

.action-btn:hover {
  opacity: 0.9;
}
</style>