<template>
  <div id="app">
    <app-header />
    <main class="main-content">
      <router-view />
    </main>
    <app-footer />
  </div>
</template>

<script>
import AppHeader from '@/components/common/Header.vue';
import AppFooter from '@/components/common/Footer.vue';

export default {
  name: 'App',
  components: {
    AppHeader,
    AppFooter
  }
};
</script>

<style>
/* 全局样式 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  color: #333;
  background-color: #fff;
  line-height: 1.5;
}

.main-content {
  min-height: calc(100vh - 200px);
}

a {
  text-decoration: none;
  color: #ff6600;
}

h1, h2, h3, h4, h5, h6 {
  margin-top: 0;
}

button {
  cursor: pointer;
}

/* 工具类 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.text-center {
  text-align: center;
}

.mt-20 {
  margin-top: 20px;
}

.mb-20 {
  margin-bottom: 20px;
}
</style>