import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

export default {
// 获取商铺列表
getShopList(params) {
return axios.get(`${BASE_URL}/shops`, { params });
},

// 获取商铺详情
getShopById(id) {
return axios.get(`${BASE_URL}/shops/${id}`);
},

// 商铺登录
login(shopName, password) {
return axios.post(`${BASE_URL}/shops/login`, { shopName, password });
},

// 商铺注册
register(shopData) {
return axios.post(`${BASE_URL}/shops/register`, shopData);
},

// 更新商铺信息
updateShop(id, shopData) {
return axios.put(`${BASE_URL}/shops/${id}`, shopData);
},

// 更新商铺状态
updateStatus(id, status) {
return axios.put(`${BASE_URL}/shops/${id}/status?status=${status}`);
},

// 删除商铺
deleteShop(id, shopName, password) {
return axios.delete(
`${BASE_URL}/shops/${id}?shopName=${encodeURIComponent(shopName)}&password=${encodeURIComponent(password)}`
);
}
};