<template>
  <div class="home">
    <h1>美食地图</h1>

    <shop-filter @filter-change="handleFilterChange" />

    <div class="shop-list">
      <shop-card
          v-for="shop in shops"
          :key="shop.shopId"
          :shop="shop"
          @click="viewShopDetail(shop.shopId)"
      />
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="shops.length === 0 && !loading" class="no-data">暂无数据</div>
  </div>
</template>

<script>
import ShopFilter from '@/components/shop/ShopFilter.vue';
import ShopCard from '@/components/shop/ShopCard.vue';
import shopApi from '@/api/shop';

export default {
  name: 'Home',
  components: {
    ShopFilter,
    ShopCard
  },
  data() {
    return {
      shops: [],
      loading: false,
      filters: {
        category: '',
        district: '',
        sortField: 'composite_score'
      }
    };
  },
  created() {
    this.fetchShops();
  },
  methods: {
    async fetchShops() {
      this.loading = true;
      try {
        const response = await shopApi.getShopList(this.filters);
        this.shops = response.data.data;
      } catch (error) {
        console.error('获取商铺列表失败', error);
      } finally {
        this.loading = false;
      }
    },
    handleFilterChange(filters) {
      this.filters = { ...this.filters, ...filters };
      this.fetchShops();
    },
    viewShopDetail(shopId) {
      this.$router.push(`/shop/${shopId}`);
    }
  }
};
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.shop-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.loading, .no-data {
  text-align: center;
  margin-top: 40px;
  color: #666;
}
</style>