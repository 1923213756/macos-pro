const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
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
    }
})