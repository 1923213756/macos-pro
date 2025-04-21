// 全局请求拦截器，处理JWT过期
function fetchWithTokenHandling(url, options = {}) {
    // 添加token
    const token = localStorage.getItem('token');
    if (token) {
        options.headers = {
            ...options.headers,
            'Authorization': `Bearer ${token}`
        };
    }

    return fetch(url, options)
        .then(response => {
            // 检查是否有新token
            const newToken = response.headers.get('X-New-Token');
            if (newToken) {
                localStorage.setItem('token', newToken);
            }

            // 处理401错误
            if (response.status === 401) {
                return response.json().then(data => {
                    if (data.expired) {
                        localStorage.clear();
                        window.location.href = 'index.html';
                        return Promise.reject('Session expired');
                    }
                    return Promise.reject(data);
                });
            }
            return response;
        })
        .then(response => {
            if (response.status === 204) return {};
            return response.json();
        })
        .catch(error => {
            if (error === 'Session expired') return Promise.reject(error);
            console.error('请求错误:', error);
            return Promise.reject(error);
        });
}

// 显示消息提示
function showMessage(elementId, message, isError = false) {
    const messageElement = document.getElementById(elementId);
    messageElement.textContent = message;
    messageElement.style.display = 'block';
    messageElement.className = isError ? 'message error' : 'message success';

    // 5秒后自动隐藏消息
    setTimeout(() => {
        messageElement.style.display = 'none';
    }, 5000);
}

// 日期格式化函数
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}