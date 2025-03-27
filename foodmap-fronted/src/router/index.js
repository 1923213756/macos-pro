import { createRouter, createWebHistory } from 'vue-router';
import Home from '@/views/Home.vue';
import ShopDetail from '@/views/ShopDetail.vue';
import ShopLogin from '@/views/ShopLogin.vue';
import ShopRegister from '@/views/ShopRegister.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/shop/:id',
        name: 'ShopDetail',
        component: ShopDetail
    },
    {
        path: '/login',
        name: 'ShopLogin',
        component: ShopLogin
    },
    {
        path: '/register',
        name: 'ShopRegister',
        component: ShopRegister
    }
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

export default router;