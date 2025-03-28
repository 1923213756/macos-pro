<template>
  <footer class="app-footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h3>美食地图</h3>
          <p>发现城市中最美味的餐厅，分享您的美食体验。</p>
        </div>

        <div class="footer-section">
          <h3>快速链接</h3>
          <ul>
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/about">关于我们</router-link></li>
            <li><router-link to="/terms">使用条款</router-link></li>
            <li><router-link to="/privacy">隐私政策</router-link></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>联系我们</h3>
          <p>邮箱: contact@foodmap.com</p>
          <p>电话: (123) 456-7890</p>
          <div class="social-icons">
            <a href="#" class="social-icon">微博</a>
            <a href="#" class="social-icon">微信</a>
            <a href="#" class="social-icon">QQ</a>
          </div>
        </div>
      </div>

      <div class="footer-bottom">
        <p>&copy; 2025 美食地图. 保留所有权利.</p>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'AppFooter'
};
</script>

<style scoped>
.app-footer {
  background-color: #f8f8f8;
  padding: 40px 0 20px;
  margin-top: 60px;
  border-top: 1px solid #eee;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
}

.footer-section h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.footer-section p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 10px;
}

.footer-section ul {
  list-style: none;
  padding: 0;
}

.footer-section ul li {
  margin-bottom: 8px;
}

.footer-section ul li a {
  color: #666;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-section ul li a:hover {
  color: #ff6600;
}

.social-icons {
  display: flex;
  gap: 15px;
  margin-top: 15px;
}

.social-icon {
  color: #666;
  text-decoration: none;
  transition: color 0.2s;
}

.social-icon:hover {
  color: #ff6600;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 14px;
}
</style>