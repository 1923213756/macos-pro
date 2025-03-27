<template>
  <div class="login-page">
    <div class="login-container">
      <h1>商铺登录</h1>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="shopName">商铺名称</label>
          <input
              type="text"
              id="shopName"
              v-model="shopName"
              required
              placeholder="请输入商铺名称"
          >
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
              type="password"
              id="password"
              v-model="password"
              required
              placeholder="请输入密码"
          >
        </div>

        <div class="form-actions">
          <button type="submit" class="btn-login" :disabled="isLoading">
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
          <router-link to="/register" class="register-link">
            没有账号？立即注册
          </router-link>
        </div>
      </form>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script>
import shopApi from '@/api/shop';

export default {
  name: 'ShopLogin',
  data() {
    return {
      shopName: '',
      password: '',
      isLoading: false,
      errorMessage: ''
    };
  },
  methods: {
    async handleLogin() {
      this.isLoading = true;
      this.errorMessage = '';

      try {
        const response = await shopApi.login(this.shopName, this.password);

        // 登录成功，保存用户信息
        const shopData = response.data.data;
        localStorage.setItem('currentShop', JSON.stringify(shopData));

        // 重定向到首页或商铺管理页面
        this.$router.push('/shop/manage');
      } catch (error) {
        console.error('登录失败', error);
        this.errorMessage = error.response?.data?.message || '登录失败，请检查用户名和密码';
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 180px);
  background-color: #f8f8f8;
  padding: 20px;
}

.login-container {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-weight: 500;
  color: #555;
}

input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 10px;
}

.btn-login {
  background-color: #ff6600;
  color: white;
  border: none;
  padding: 12px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-login:disabled {
  background-color: #ffaa77;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  color: #666;
  text-decoration: none;
  font-size: 14px;
}

.register-link:hover {
  color: #ff6600;
}

.error-message {
  margin-top: 20px;
  padding: 10px;
  background-color: #fff0f0;
  color: #e53935;
  border-radius: 4px;
  text-align: center;
}
</style>