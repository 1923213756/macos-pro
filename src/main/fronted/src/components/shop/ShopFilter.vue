<template>
  <div class="shop-filter">
    <div class="filter-section">
      <h3>分类</h3>
      <div class="filter-options">
        <button
            v-for="cat in categories"
            :key="cat"
            :class="{ active: category === cat }"
            @click="setCategory(cat)">
          {{ cat }}
        </button>
      </div>
    </div>

    <div class="filter-section">
      <h3>地区</h3>
      <div class="filter-options">
        <button
            v-for="dist in districts"
            :key="dist"
            :class="{ active: district === dist }"
            @click="setDistrict(dist)">
          {{ dist }}
        </button>
      </div>
    </div>

    <div class="filter-section">
      <h3>排序</h3>
      <div class="filter-options">
        <button
            v-for="sort in sortOptions"
            :key="sort.value"
            :class="{ active: sortField === sort.value }"
            @click="setSortField(sort.value)">
          {{ sort.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShopFilter',
  data() {
    return {
      category: '',
      district: '',
      sortField: 'composite_score',

      categories: ['全部', '中餐', '西餐', '火锅', '快餐', '料理', '甜品'],
      districts: ['全部', '海淀区', '朝阳区', '东城区', '西城区', '丰台区'],
      sortOptions: [
        { label: '综合评分', value: 'composite_score' },
        { label: '口味评分', value: 'taste_score' },
        { label: '环境评分', value: 'environment_score' },
        { label: '服务评分', value: 'service_score' }
      ]
    };
  },
  methods: {
    setCategory(category) {
      this.category = category === '全部' ? '' : category;
      this.emitFilterChange();
    },
    setDistrict(district) {
      this.district = district === '全部' ? '' : district;
      this.emitFilterChange();
    },
    setSortField(sortField) {
      this.sortField = sortField;
      this.emitFilterChange();
    },
    emitFilterChange() {
      this.$emit('filter-change', {
        category: this.category,
        district: this.district,
        sortField: this.sortField
      });
    }
  }
};
</script>

<style scoped>
.shop-filter {
  background: #f8f8f8;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
}

.filter-section {
  margin-bottom: 15px;
}

.filter-section h3 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #333;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-options button {
  padding: 5px 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-options button.active {
  background: #ff6600;
  color: white;
  border-color: #ff6600;
}
</style>