// 加载商铺评论
function loadShopReviews(shopId, page = 1, size = 5) {
    fetchWithTokenHandling(`http://localhost:8080/api/reviews/restaurant/${shopId}?current=${page}&size=${size}`)
        .then(data => {
            const reviewsContainer = document.getElementById('shop-reviews');

            if (!data.records || data.records.length === 0) {
                reviewsContainer.innerHTML = `<div class="no-reviews">这家店铺还没有评论，成为第一个评论的人吧！</div>`;
                return;
            }

            // 渲染评论列表
            let html = '';
            data.records.forEach(review => {
                const isOwner = currentUser && review.userId === currentUser.userId;
                html += `
                <div class="review-item" data-id="${review.id}">
                    <div class="review-header">
                        <span class="review-author">${review.username || '匿名用户'}</span>
                        <span class="review-date">${formatDate(review.createdAt)}</span>
                    </div>
                    <div class="review-ratings">
                        <div class="review-rating">
                            <span class="rating-label">综合评分:</span>
                            <span class="stars-container">${getStarsHtml(review.rating)}</span>
                            <span class="rating-value">${review.rating.toFixed(1)}</span>
                        </div>
                        <div class="review-rating-details">
                            <span>环境: ${review.environmentRating?.toFixed(1) || '无'}</span> |
                            <span>服务: ${review.serviceRating?.toFixed(1) || '无'}</span> |
                            <span>口味: ${review.tasteRating?.toFixed(1) || '无'}</span>
                        </div>
                    </div>
                    <div class="review-content">${review.content}</div>
                    <div class="review-actions">
                        <div class="review-like ${review.userLiked ? 'active' : ''}" onclick="toggleReviewLike(${review.id})">
                            <i class="like-icon">♥</i> <span class="like-count">${review.likeCount || 0}</span>
                        </div>
                        ${isOwner ? `
                            <div class="review-edit-actions">
                                <span class="review-edit" onclick="editReview(${review.id})">编辑</span>
                                <span class="review-delete" onclick="deleteReview(${review.id})">删除</span>
                            </div>
                        ` : ''}
                    </div>
                </div>
                `;
            });

            reviewsContainer.innerHTML = html;

            // 渲染分页
            renderPagination(data, 'shop-reviews-pagination', page => loadShopReviews(shopId, page, size));
        })
        .catch(error => {
            if (error !== 'Session expired') {
                document.getElementById('shop-reviews').innerHTML =
                    `<div class="error">加载评论失败: ${error.message || error}</div>`;
            }
        });
}

// 渲染分页控件
function renderPagination(pageData, containerId, callbackFn) {
    const container = document.getElementById(containerId);

    if (!pageData || pageData.total <= pageData.size) {
        container.innerHTML = '';
        return;
    }

    const totalPages = Math.ceil(pageData.total / pageData.size);
    let html = '';

    // 上一页按钮
    if (pageData.current > 1) {
        html += `<span class="pagination-btn" onclick="(${callbackFn})(${pageData.current - 1})">«</span>`;
    }

    // 页码按钮
    const startPage = Math.max(1, pageData.current - 2);
    const endPage = Math.min(totalPages, pageData.current + 2);

    for (let i = startPage; i <= endPage; i++) {
        html += `<span class="pagination-btn ${i === pageData.current ? 'active' : ''}" 
                onclick="(${callbackFn})(${i})">
                ${i}
            </span>`;
    }

    // 下一页按钮
    if (pageData.current < totalPages) {
        html += `<span class="pagination-btn" onclick="(${callbackFn})(${pageData.current + 1})">»</span>`;
    }

    container.innerHTML = html;
}

// 生成星级评分的HTML
function getStarsHtml(score) {
    const normalizedScore = Math.min(Math.max(score, 0), 5);
    const fullStars = Math.floor(normalizedScore);
    const halfStar = normalizedScore - fullStars >= 0.5;
    const emptyStars = 5 - fullStars - (halfStar ? 1 : 0);

    let html = '';
    for (let i = 0; i < fullStars; i++) html += '★';
    if (halfStar) html += '★';
    for (let i = 0; i < emptyStars; i++) html += '☆';

    return html;
}

// 初始化评论相关事件
function initReviewEvents() {
    // 星级评分选择处理
    setupRatingStars('environment-rating-input', 'review-environment-rating');
    setupRatingStars('service-rating-input', 'review-service-rating');
    setupRatingStars('taste-rating-input', 'review-taste-rating');

    // 评论模态框关闭按钮
    document.getElementById('close-review-modal').addEventListener('click', () => {
        document.getElementById('review-modal').style.display = 'none';
    });

    // 点击模态框外部关闭
    document.getElementById('review-modal').addEventListener('click', function(event) {
        if (event.target === this) {
            this.style.display = 'none';
        }
    });

    // 评论表单提交处理
    document.getElementById('review-form').addEventListener('submit', handleReviewSubmit);

    // 写评论按钮
    document.getElementById('write-review-btn').addEventListener('click', openReviewForm);
}

// 设置星级评分功能
function setupRatingStars(containerId, inputId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    const stars = container.querySelectorAll('.star');
    stars.forEach(star => {
        star.addEventListener('click', function() {
            const value = this.getAttribute('data-value');
            document.getElementById(inputId).value = value;

            stars.forEach(s => {
                if (parseInt(s.getAttribute('data-value')) <= parseInt(value)) {
                    s.classList.add('selected');
                } else {
                    s.classList.remove('selected');
                }
            });
        });
    });
}

// 更多评论相关函数...
// 包括editReview(), deleteReview(), toggleReviewLike(), loadUserReviews()等

// 页面加载完成后初始化评论组件
document.addEventListener('DOMContentLoaded', function() {
    // 这里假设modals.html已加载完成
    setTimeout(() => {
        initReviewEvents();
    }, 500);
});