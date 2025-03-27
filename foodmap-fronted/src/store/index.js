import { createStore } from 'vuex'

export default createStore({
    state: {
        currentShop: null,
        isLoading: false,
        categories: ['中餐', '西餐', '火锅', '快餐', '料理', '甜品'],
        districts: ['海淀区', '朝阳区', '东城区', '西城区', '丰台区'],
    },
    getters: {
        isLoggedIn: state => !!state.currentShop,
        getShopData: state => state.currentShop,
    },
    mutations: {
        SET_CURRENT_SHOP(state, shop) {
            state.currentShop = shop;
        },
        CLEAR_CURRENT_SHOP(state) {
            state.currentShop = null;
        },
        SET_LOADING(state, isLoading) {
            state.isLoading = isLoading;
        }
    },
    actions: {
        initializeApp({ commit }) {
            // 从本地存储恢复用户登录状态
            const shopData = localStorage.getItem('currentShop');
            if (shopData) {
                try {
                    commit('SET_CURRENT_SHOP', JSON.parse(shopData));
                } catch (e) {
                    localStorage.removeItem('currentShop');
                }
            }
        },
        login({ commit }, shop) {
            commit('SET_CURRENT_SHOP', shop);
            localStorage.setItem('currentShop', JSON.stringify(shop));
        },
        logout({ commit }) {
            commit('CLEAR_CURRENT_SHOP');
            localStorage.removeItem('currentShop');
        }
    }
})