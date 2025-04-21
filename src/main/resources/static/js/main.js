// 全局状态
let currentUser = null;

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', async function() {
    try {
        // 加载组件
        await loadComponents();

        // 检查是否已登录
        checkAuthStatus();

        // 初始化事件处理
        initEvents();

    } catch (error) {
        console.error("初始化仪表盘出错:", error);
        handleInitError(error);
    }
});

// 加载组件
async function loadComponents() {
    try {
        // 加载侧边栏
        const sidebarResponse = await fetch('components/sidebar.html');
        if (!sidebarResponse.ok) throw new Error('无法加载侧边栏');
        document.getElementById('sidebar-container').innerHTML = await sidebarResponse.text();

        // 加载模态框
        const modalsResponse = await fetch('components/modals.html');
        if (!modalsResponse.ok) throw new Error('无法加载模态框');
        document.getElementById('modals-container').innerHTML = await modalsResponse.text();

        return true;
    } catch (error) {
        console.error('加载组件失败:', error);
        throw error;
    }
}

// 检查登录状态
function checkAuthStatus() {
    const token = localStorage.getItem('token');
    const userInfo = localStorage.getItem('userInfo');
    const shopInfo = localStorage.getItem('shopInfo');

    console.log("检查登录信息:");
    console.log("- token:", token ? "存在" : "不存在");
    console.log("- userInfo:", userInfo ? "存在" : "不存在");
    console.log("- shopInfo:", shopInfo ? "存在" : "不存在");

    if (!token || (!userInfo && !shopInfo)) {
        // 未登录，重定向到登录页
        console.log("未找到有效的登录信息，跳转到登录页");
        window.location.href = 'index.html';
        return;
    }

    // 根据存储的信息判断用户类型
    if (userInfo) {
        // 普通用户
        console.log("检测到用户登录");
        initUserDashboard(JSON.parse(userInfo));
    } else if (shopInfo) {
        // 商铺管理员
        console.log("检测到商铺登录");
        initShopDashboard(JSON.parse(shopInfo));
    }
}

// 初始化普通用户的仪表盘
function initUserDashboard(user) {
    currentUser = user;

    // 设置用户信息
    document.getElementById('user-name').textContent = user.userName;
    document.getElementById('user-avatar').textContent = user.userName.charAt(0);

    // 显示用户个人信息表单
    document.getElementById('user-profile-section').style.display = 'block';
    document.getElementById('profile-username').value = user.userName;
    document.getElementById('profile-phone').value = user.phone || '';

    // 隐藏商铺管理菜单
    document.querySelectorAll('.shop-admin-section').forEach(el => {
        el.style.display = 'none';
    });

    // 设置欢迎信息
    document.getElementById('welcome-message').innerHTML = `
        <p>欢迎回来，${user.userName}！</p>
        <p>您可以浏览商铺、查看商铺详情，享受美食之旅。</p>
    `;

    // 加载热门商铺
    loadPopularShops();
}

// 初始化商铺管理员的仪表盘
function initShopDashboard(shop) {
    currentShop = shop;

    // 设置商铺信息
    document.getElementById('user-name').textContent = shop.shopName;
    document.getElementById('user-avatar').textContent = shop.shopName.charAt(0);

    // 显示商铺管理菜单
    document.querySelectorAll('.shop-admin-section').forEach(el => {
        el.style.display = 'block';
    });

    // 显示商铺个人信息
    document.getElementById('shop-profile-section').style.display = 'block';

    // 加载商铺详细信息
    loadShopDetails(shop.shopId);

    // 设置欢迎信息
    document.getElementById('welcome-message').innerHTML = `
        <p>欢迎回来，${shop.shopName}！</p>
        <p>您可以管理商铺信息、更新营业状态。</p>
    `;

    // 加载热门商铺
    loadPopularShops();
}

// 初始化事件处理
function initEvents() {
    // 退出登录
    document.getElementById('logout-btn').addEventListener('click', function(e) {
        e.preventDefault();
        localStorage.clear();
        window.location.href = 'index.html';
    });

    // 修改密码表单处理
    document.getElementById('password-form').addEventListener('submit', function(e) {
        e.preventDefault();
        updatePassword();
    });

    // 更新用户信息
    document.getElementById('user-profile-form').addEventListener('submit', function(e) {
        e.preventDefault();
        updateUserProfile();
    });

    // 菜单项点击事件
    document.querySelectorAll('.nav-links a, .sidebar-menu a').forEach(link => {
        link.addEventListener('click', function(e) {
            const viewId = this.getAttribute('onclick')?.match(/showView\('([^']+)'\)/)?.[1];
            if (viewId === 'my-reviews-view') {
                loadUserReviews();
            }
        });
    });
}

// 显示视图函数
function showView(viewId) {
    // 隐藏所有视图
    document.querySelectorAll('.view').forEach(view => {
        view.classList.remove('active');
    });

    // 显示指定视图
    document.getElementById(viewId).classList.add('active');

    // 更新导航链接状态
    document.querySelectorAll('.nav-links a, .sidebar-menu a').forEach(link => {
        link.classList.remove('active');
    });

    // 找到对应的导航链接并激活
    document.querySelectorAll(`.nav-links a[onclick*="${viewId}"], .sidebar-menu a[onclick*="${viewId}"]`).forEach(link => {
        link.classList.add('active');
    });
}

// 修改密码
function updatePassword() {
    // 获取表单数据
    const currentPassword = document.getElementById('current-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;

    // 表单验证
    if (!currentPassword || !newPassword || !confirmPassword) {
        showMessage('password-message', '所有密码字段都必须填写', true);
        return;
    }

    if (newPassword !== confirmPassword) {
        showMessage('password-message', '新密码与确认密码不匹配', true);
        return;
    }

    // 密码强度验证
    if (newPassword.length < 8) {
        showMessage('password-message', '新密码长度必须至少为8个字符', true);
        return;
    }

    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,}$/.test(newPassword)) {
        showMessage('password-message', '密码必须包含至少一个大写字母、一个小写字母和一个数字', true);
        return;
    }

    // 构建请求数据
    const requestData = {
        oldPassword: currentPassword,
        newPassword: newPassword
    };

    // 确定用户类型并获取正确的ID
    let url;
    if (currentShop) {
        // 商铺用户
        url = `http://localhost:8080/api/shops/${currentShop.shopId}/password`;
    } else if (currentUser) {
        // 普通用户
        url = `http://localhost:8080/api/users/${currentUser.userId}/password`;
    } else {
        showMessage('password-message', '无法确定当前用户类型', true);
        return;
    }

    // 发送请求
    fetchWithTokenHandling(url, {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(requestData)
    })
        .then(data => {
            if (data.code === 200) {
                showMessage('password-message', '密码修改成功！请使用新密码重新登录。');
                document.getElementById('current-password').value = '';
                document.getElementById('new-password').value = '';
                document.getElementById('confirm-password').value = '';
                setTimeout(() => {
                    localStorage.clear();
                    window.location.href = 'index.html';
                }, 3000);
            } else {
                showMessage('password-message', `密码修改失败: ${data.message || '未知错误'}`, true);
            }
        })
        .catch(error => {
            showMessage('password-message', `请求错误: ${error.message}`, true);
        });
}

// 更新用户信息
function updateUserProfile() {
    // 这里需要一个更新用户信息的API，但API文档中没有提供
    showMessage('user-profile-message', '用户信息更新功能尚未实现');
}

// 处理初始化错误
function handleInitError(error) {
    document.body.innerHTML = `
        <div style="margin: 50px auto; max-width: 600px; padding: 20px; border: 1px solid #f44336; background-color: #fff;">
            <h2 style="color: #f44336;">页面加载错误</h2>
            <p>初始化过程中发生错误。</p>
            <p>错误信息: ${error.message}</p>
            <button onclick="localStorage.clear(); window.location.href='index.html';"
                style="background: #4CAF50; color: white; padding: 10px 15px; border: none; cursor: pointer; margin-top: 15px;">
                返回登录页
            </button>
        </div>
    `;
}