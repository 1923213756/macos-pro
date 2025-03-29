<template>
  <div class="shop-card" @click="$emit('click')">
    <div class="shop-info">
      <h3 class="shop-name">{{ shop.shopName }}</h3>
      <div class="shop-rating">
        <span class="score">{{ shop.compositeScore.toFixed(1) }}</span>
        <span class="stars">
          <i v-for="n in 5" :key="n" class="star"
             :class="{ 'active': n <= Math.round(shop.compositeScore) }">★</i>
        </span>
      </div>
      <div class="shop-category">{{ shop.category }}</div>
      <div class="shop-address">{{ shop.district }} · {{ shop.address }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShopCard',
  props: {
    shop: {
      type: Object,
      required: true
    }
  }
};
</script>

<style scoped>
.shop-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.3s;
}

.shop-card:hover {
  transform: translateY(-5px);
}

.shop-info {
  padding: 15px;
}

.shop-name {
  margin: 0 0 8px;
  font-size: 18px;
}

.shop-rating {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.score {
  font-weight: bold;
  margin-right: 8px;
  color: #ff6600;
}

.stars {
  color: #ddd;
}

.star.active {
  color: #ff6600;
}

.shop-category, .shop-address {
  color: #666;
  font-size: 14px;
  margin-top: 5px;
}
</style>