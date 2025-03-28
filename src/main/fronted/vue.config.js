const path = require('path');

module.exports = {
    transpileDependencies: true,
    // 输出到Spring Boot静态资源目录
    outputDir: '../resources/static',
    // 开发服务器代理配置
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    // 配置路径别名
    configureWebpack: {
        resolve: {
            alias: {
                '@': path.resolve(__dirname, 'src')
            }
        }
    }
};