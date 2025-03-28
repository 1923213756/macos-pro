<template>
  <div class="shop-detail" v-if="shop">
    <div class="shop-header">
      <h1>{{ shop.shopName }}</h1>
      <div class="shop-stats">
        <div class="rating">
          <span class="score">{{ shop.compositeScore.toFixed(1) }}</span>
          <div class="detail-scores">
            <div>口味: {{ shop.tasteScore }}</div>
            <div>环境: {{ shop.environmentScore }}</div>
            <div>服务: {{ shop.serviceScore }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="shop-info-panel">
      <div class="info-item">
        <i class="icon">📍</i>
        <span>{{ shop.address }}</span>
      </div>
      <div class="info-item">
        <i class="icon">📞</i>
        <span>{{ shop.contactTel }}</span>
      </div>
      <div class="info-item">
        <i class="icon">🕒</i>
        <span>{{ shop.businessHours }}</span>
      </div>
      <div class="info-item">
        <i class="icon">🍴</i>
        <span>{{ shop.category }}</span>
      </div>
    </div>

    <div class="shop-description">
      <h2>商铺简介</h2>
      <p>{{ shop.description || '暂无简介' }}</p>
    </div>

    <!-- 可以添加更多内容，如菜单、评论等 -->
  </div>
  <div v-else-if="loading" class="loading">加载中...</div>
  <div v-else class="error">商铺信息加载失败</div>
</template>

<script>
import shopApi from '@/api/shop';

export default {
  name: 'ShopDetail',
  data() {
    return {
      shop: null,
      loading: true
    };
  },
  created() {
    this.fetchShopDetail();
  },
  methods: {
    async fetchShopDetail() {
      const shopId = this.$route.params.id;
      try {
        const response = await shopApi.getShopById(shopId);
        this.shop = response.data.data;
      } catch (error) {
        console.error('获取商铺详情失败', error);
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.shop-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.shop-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.shop-stats {
  display: flex;
  align-items: center;
}

.rating {
  display: flex;
  align-items: center;
}

.score {
  font-size: 32px;
  font-weight: bold;
  color: #ff6600;
  margin-right: 15px;
}

.detail-scores {
  color: #666;
}

.shop-info-panel {
  background: #f8f8f8;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.icon {
  margin-right: 15px;
  font-size: 20px;
}

.shop-description {
  margin-bottom: 30px;
}

.shop-description h2 {
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 15px;
}

.loading, .error {
  text-align: center;
  margin-top: 100px;
  font-size: 18px;
  color: #666;
}

.error {
  color: #e53935;
}
</style>