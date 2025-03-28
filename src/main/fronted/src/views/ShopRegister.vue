<template>
  <div class="register-page">
    <div class="register-container">
      <h1>商铺注册</h1>

      <form @submit.prevent="handleRegister" class="register-form">
        <div class="form-group">
          <label for="shopName">商铺名称 *</label>
          <input
              type="text"
              id="shopName"
              v-model="shopData.shopName"
              required
              placeholder="请输入商铺名称"
          >
        </div>

        <div class="form-group">
          <label for="password">密码 *</label>
          <input
              type="password"
              id="password"
              v-model="shopData.password"
              required
              placeholder="请输入密码"
          >
        </div>

        <div class="form-group">
          <label for="confirmPassword">确认密码 *</label>
          <input
              type="password"
              id="confirmPassword"
              v-model="confirmPassword"
              required
              placeholder="请再次输入密码"
          >
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="category">分类 *</label>
            <select id="category" v-model="shopData.category" required>
              <option value="">请选择分类</option>
              <option v-for="cat in categories" :key="cat" :value="cat">
                {{ cat }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label for="district">所在地区 *</label>
            <select id="district" v-model="shopData.district" required>
              <option value="">请选择地区</option>
              <option v-for="dist in districts" :key="dist" :value="dist">
                {{ dist }}
              </option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label for="address">详细地址 *</label>
          <input
              type="text"
              id="address"
              v-model="shopData.address"
              required
              placeholder="请输入详细地址"
          >
        </div>

        <div class="form-group">
          <label for="contactTel">联系电话 *</label>
          <input
              type="tel"
              id="contactTel"
              v-model="shopData.contactTel"
              required
              placeholder="请输入联系电话"
          >
        </div>

        <div class="form-group">
          <label for="businessHours">营业时间</label>
          <input
              type="text"
              id="businessHours"
              v-model="shopData.businessHours"
              placeholder="例如：周一至周日 10:00-22:00"
          >
        </div>

        <div class="form-group">
          <label for="description">商铺简介</label>
          <textarea
              id="description"
              v-model="shopData.description"
              rows="4"
              placeholder="请输入商铺简介"
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn-register" :disabled="isLoading">
            {{ isLoading ? '注册中...' : '注册' }}
          </button>
          <router-link to="/login" class="login-link">
            已有账号？立即登录
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
  name: 'ShopRegister',
  data() {
    return {
      shopData: {
        shopName: '',
        password: '',
        category: '',
        district: '',
        address: '',
        contactTel: '',
        businessHours: '',
        description: '',
        // 默认值
        status: 1,
        tasteScore: 5.0,
        environmentScore: 5.0,
        serviceScore: 5.0,
        compositeScore: 5.0
      },
      confirmPassword: '',
      isLoading: false,
      errorMessage: '',
      categories: ['中餐', '西餐', '火锅', '快餐', '料理', '甜品'],
      districts: ['海淀区', '朝阳区', '东城区', '西城区', '丰台区']
    };
  },
  methods: {
    async handleRegister() {
      // 验证两次密码是否一致
      if (this.shopData.password !== this.confirmPassword) {
        this.errorMessage = '两次输入的密码不一致';
        return;
      }

      this.isLoading = true;
      this.errorMessage = '';

      try {
        await shopApi.register(this.shopData);

        // 注册成功，跳转到登录页
        this.$router.push({
          path: '/login',
          query: { registered: 'success' }
        });
      } catch (error) {
        console.error('注册失败', error);
        this.errorMessage = error.response?.data?.message || '注册失败，请稍后再试';
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f8f8f8;
  padding: 40px 20px;
}

.register-container {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 600px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

label {
  font-weight: 500;
  color: #555;
}

input, select, textarea {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 10px;
}

.btn-register {
  background-color: #ff6600;
  color: white;
  border: none;
  padding: 12px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-register:disabled {
  background-color: #ffaa77;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  color: #666;
  text-decoration: none;
  font-size: 14px;
}

.login-link:hover {
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