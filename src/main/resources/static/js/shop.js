// 全局商铺状态
let currentShop = null;

// 加载热门商铺
function loadPopularShops() {
    fetchWithTokenHandling('http://localhost:8080/api/shops?sortField=compositeScore')
        .then(data => {
            if (data.code === 200 && data.data) {
                renderShopList(data.data, 'popular-shops', 5); // 只显示前5个热门商铺
            } else {
                document.getElementById('popular-shops').innerHTML =
                    `<p>加载热门商铺失败: ${data.message || '未知错误'}</p>`;
            }
        })
        .catch(error => {
            document.getElementById('popular-shops').innerHTML = `<p>请求错误: ${error}</p>`;
        });
}

// 加载商铺列表
function loadShopList(category = '', district = '', sortField = 'compositeScore') {
    // 构建查询参数
    let url = 'http://localhost:8080/api/shops?';
    if (category) url += `category=${encodeURIComponent(category)}&`;
    if (district) url += `district=${encodeURIComponent(district)}&`;
    if (sortField) url += `sortField=${encodeURIComponent(sortField)}`;

    fetchWithTokenHandling(url)
        .then(data => {
            if (data.code === 200 && data.data) {
                renderShopList(data.data, 'filtered-shops');
            } else {
                document.getElementById('filtered-shops').innerHTML =
                    `<p>加载商铺列表失败: ${data.message || '未知错误'}</p>`;
            }
        })
        .catch(error => {
            document.getElementById('filtered-shops').innerHTML = `<p>请求错误: ${error}</p>`;
        });
}

// 渲染商铺列表
function renderShopList(shops, containerId, limit = null) {
    const container = document.getElementById(containerId);

    if (!shops || shops.length === 0) {
        container.innerHTML = '<p>没有找到符合条件的商铺</p>';
        return;
    }

    // 如果有限制，只显示指定数量的商铺
    const shopsToShow = limit ? shops.slice(0, limit) : shops;

    let html = '';
    shopsToShow.forEach(shop => {
        html += `
            <div class="shop-card" data-shop-id="${shop.shopId}">
                <div class="shop-card-image">商铺图片</div>
                <div class="shop-card-content">
                    <div class="shop-name">${shop.shopName || '未命名商铺'}</div>
                    <div class="shop-category">${shop.category || '未分类'}</div>
                    <div class="shop-address">${shop.address || '地址未提供'}</div>
                    <div class="shop-rating">评分: ${shop.compositeScore ? shop.compositeScore.toFixed(1) : '暂无评分'}</div>
                    <div class="shop-description">${shop.description || '暂无描述'}</div>
                    <a href="#" class="btn" onclick="viewShopDetails(${shop.shopId}); return false;">查看详情</a>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// 查看商铺详情
function viewShopDetails(shopId) {
    // 获取商铺详情
    fetchWithTokenHandling(`http://localhost:8080/api/shops/${shopId}`)
        .then(data => {
            if (data.code === 200) {
                const shop = data.data;

                // 设置商铺名称和ID
                const shopNameElement = document.getElementById('detail-shop-name');
                shopNameElement.textContent = shop.shopName || '未命名商铺';
                shopNameElement.setAttribute('data-shop-id', shop.shopId);

                // 构建详情内容
                let detailHTML = `
                    <div class="detail-section">
                        <div class="detail-item">
                            <div class="detail-label">商铺分类</div>
                            <div class="detail-value">${shop.category || '未设置'}</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">所在区域</div>
                            <div class="detail-value">${shop.district || '未设置'}</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">详细地址</div>
                            <div class="detail-value">${shop.address || '未设置'}</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">联系电话</div>
                            <div class="detail-value">${shop.contactTel || '未设置'}</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">营业时间</div>
                            <div class="detail-value">${shop.businessHours || '未设置'}</div>
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-section-title">商铺评分</div>
                        <div class="detail-item">
                            <div class="detail-label">综合评分</div>
                            <div class="detail-value">
                                <div class="stars-container">${getStarsHtml(shop.compositeScore || 0)}</div>
                                ${shop.compositeScore ? shop.compositeScore.toFixed(1) : '暂无评分'}
                            </div>
                        </div>
                    </div>

                    <div class="detail-section" style="border-bottom: none;">
                        <div class="detail-section-title">商铺介绍</div>
                        <div style="line-height: 1.6;">
                            ${shop.description || '该商铺暂无介绍信息'}
                        </div>
                    </div>
                `;

                document.getElementById('shop-detail-content').innerHTML = detailHTML;

                // 加载商铺评论
                loadShopReviews(shop.shopId);

                // 检查当前用户是否是商铺管理员
                const shopInfo = localStorage.getItem('shopInfo');
                let isOwner = false;
                if (shopInfo) {
                    const currentShopInfo = JSON.parse(shopInfo);
                    isOwner = (currentShopInfo.shopId === shop.shopId);
                }

                // 显示/隐藏编辑按钮
                document.getElementById('edit-shop-btn').style.display = isOwner ? 'block' : 'none';

                // 设置编辑按钮点击事件
                document.getElementById('edit-shop-btn').onclick = function() {
                    // 关闭模态框
                    document.getElementById('shop-detail-modal').style.display = 'none';
                    // 跳转到商铺编辑页面
                    showView('shop-info-view');
                    // 填充表单数据
                    document.getElementById('shop-name').value = shop.shopName || '';
                    document.getElementById('shop-address').value = shop.address || '';
                    document.getElementById('shop-tel').value = shop.contactTel || '';
                    document.getElementById('shop-hours').value = shop.businessHours || '';
                    if (shop.category) {
                        document.getElementById('shop-category').value = shop.category;
                    }
                    if (shop.district) {
                        document.getElementById('shop-district').value = shop.district;
                    }
                    document.getElementById('shop-description').value = shop.description || '';
                };

                // 设置收藏按钮功能
                document.getElementById('favorite-shop-btn').onclick = function() {
                    alert('收藏功能即将上线，敬请期待！');
                };

                // 显示模态框
                document.getElementById('shop-detail-modal').style.display = 'block';
            } else {
                alert(`获取商铺详情失败: ${data.message || '未知错误'}`);
            }
        })
        .catch(error => {
            console.error('获取商铺详情错误:', error);
            alert(`错误: ${error.message}`);
        });
}

// 删除商铺功能
function deleteShop(shopId) {
    // 首先显示确认对话框
    if (!confirm('确定要删除此商铺吗？此操作不可撤销！')) {
        return;
    }

    // 请求输入商铺名称和密码
    const shopName = prompt('请输入商铺名称以确认删除：');
    if (!shopName) return;

    const password = prompt('请输入商铺密码以确认删除：');
    if (!password) return;

    // 如果用户未输入任何内容，显示错误并返回
    if (!shopName.trim() || !password.trim()) {
        alert('商铺名称和密码不能为空');
        return;
    }

    // 显示正在处理的消息
    alert('正在验证信息并处理删除请求...');

    // 构建URL，包含所需的查询参数
    const url = `http://localhost:8080/api/shops/${shopId}?shopName=${encodeURIComponent(shopName)}&password=${encodeURIComponent(password)}`;

    fetchWithTokenHandling(url, { method: 'DELETE' })
        .then(data => {
            if (data.code === 200) {
                alert('商铺已成功删除！');
                // 商铺被删除，退出登录
                localStorage.clear();
                window.location.href = 'index.html';
            } else {
                alert(`删除失败: ${data.message || '未知错误'}`);
            }
        })
        .catch(error => {
            console.error('删除商铺错误:', error);
            alert(`错误: ${error.message}`);
        });
}

// 加载商铺详情
function loadShopDetails(shopId) {
    fetchWithTokenHandling(`http://localhost:8080/api/shops/${shopId}`)
        .then(data => {
            if (data.code === 200 && data.data) {
                const shop = data.data;
                // 更新商铺信息表单
                document.getElementById('shop-name').value = shop.shopName || '';
                document.getElementById('shop-address').value = shop.address || '';
                document.getElementById('shop-tel').value = shop.contactTel || '';
                document.getElementById('shop-hours').value = shop.businessHours || '';
                if (shop.category) {
                    document.getElementById('shop-category').value = shop.category;
                }
                if (shop.district) {
                    document.getElementById('shop-district').value = shop.district;
                }
                document.getElementById('shop-description').value = shop.description || '';
                // 更新营业状态
                const statusText = shop.status === 1 ? '营业中' : '休息中';
                document.getElementById('current-status').textContent = statusText;
                document.getElementById('status-select').value = shop.status.toString();
            } else {
                showMessage('shop-info-message', `加载商铺信息失败: ${data.message || '未知错误'}`, true);
            }
        })
        .catch(error => {
            showMessage('shop-info-message', `请求错误: ${error}`, true);
        });
}

// 初始化商铺相关事件
function initShopEvents() {
    // 商铺详情模态框关闭按钮
    document.getElementById('close-detail-modal').addEventListener('click', function() {
        document.getElementById('shop-detail-modal').style.display = 'none';
    });

    // 点击模态框外部区域关闭
    document.getElementById('shop-detail-modal').addEventListener('click', function(event) {
        if (event.target === this) {
            this.style.display = 'none';
        }
    });

    // 应用筛选按钮
    document.getElementById('apply-filters').addEventListener('click', function() {
        const category = document.getElementById('category-filter').value;
        const district = document.getElementById('district-filter').value;
        const sortField = document.getElementById('sort-filter').value;
        loadShopList(category, district, sortField);
    });

    // 更新商铺信息
    document.getElementById('shop-info-form').addEventListener('submit', function(e) {
        e.preventDefault();
        updateShopInfo();
    });

    // 更新商铺状态
    document.getElementById('shop-status-form').addEventListener('submit', function(e) {
        e.preventDefault();
        updateShopStatus();
    });

    // 删除商铺按钮
    document.getElementById('shop-delete-btn').addEventListener('click', function() {
        if (currentShop && currentShop.shopId) {
            deleteShop(currentShop.shopId);
        } else {
            alert('无法识别当前商铺信息');
        }
    });
}

// 更新商铺信息
function updateShopInfo() {
    const shopData = {
        shopName: document.getElementById('shop-name').value,
        address: document.getElementById('shop-address').value,
        contactTel: document.getElementById('shop-tel').value,
        businessHours: document.getElementById('shop-hours').value,
        category: document.getElementById('shop-category').value,
        district: document.getElementById('shop-district').value,
        description: document.getElementById('shop-description').value
    };

    fetchWithTokenHandling(`http://localhost:8080/api/shops/${currentShop.shopId}`, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(shopData)
    })
        .then(data => {
            if (data.code === 200) {
                showMessage('shop-info-message', '商铺信息更新成功！');
                // 更新本地存储的商铺信息
                currentShop = { ...currentShop, ...shopData };
                localStorage.setItem('shopInfo', JSON.stringify(currentShop));
            } else {
                showMessage('shop-info-message', `更新失败: ${data.message || '未知错误'}`, true);
            }
        })
        .catch(error => {
            showMessage('shop-info-message', `请求错误: ${error}`, true);
        });
}

// 更新商铺状态
function updateShopStatus() {
    const status = document.getElementById('status-select').value;

    fetchWithTokenHandling(`http://localhost:8080/api/shops/${currentShop.shopId}/status?status=${status}`, {
        method: 'PUT'
    })
        .then(data => {
            if (data.code === 200) {
                showMessage('shop-status-message', '商铺营业状态更新成功！');
                // 更新显示的当前状态
                const statusText = status === '1' ? '营业中' : '休息中';
                document.getElementById('current-status').textContent = statusText;
                // 更新本地存储的商铺信息
                currentShop.status = parseInt(status);
                localStorage.setItem('shopInfo', JSON.stringify(currentShop));
            } else {
                showMessage('shop-status-message', `更新失败: ${data.message || '未知错误'}`, true);
            }
        })
        .catch(error => {
            showMessage('shop-status-message', `请求错误: ${error}`, true);
        });
}

// 页面加载完成后初始化商铺相关组件
document.addEventListener('DOMContentLoaded', function() {
    // 延迟初始化以确保DOM已完全加载
    setTimeout(() => {
        initShopEvents();
    }, 500);
});