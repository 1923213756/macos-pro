from flask import Flask, request, jsonify
from config import logger
from analyzer import analyze_reviews_batch
import time
import json

app = Flask(__name__)

@app.route('/analyze_batch', methods=['POST'])
def analyze_batch():
    """API端点：批量分析多条评论，提供汇总统计"""
    start_time = time.time()
    data = request.json
    reviews = data.get('reviews', [])
    
    # 记录请求详情
    logger.info(f"收到批量分析请求 - {len(reviews)} 条评论")
    if reviews:
        logger.info(f"样本评论: {reviews[0][:50]}...")
    
    try:
        logger.info("开始评论分析流程...")
        result = analyze_reviews_batch(reviews)
        
        # 记录处理结果摘要
        process_time = time.time() - start_time
        logger.info(f"分析完成! 处理时间: {process_time:.2f}秒")
        logger.info(f"生成摘要: {result['summary'][:100]}...")
        logger.info(f"识别短语统计: {len(result['phrase_stats'])} 个短语")
        
        # 打印前3个最重要的短语统计
        if result.get('phrase_stats'):
            for i, stat in enumerate(result['phrase_stats'][:3]):
                logger.info(f"Top {i+1} 短语: {stat['phrase']} - 情感:{stat['sentiment']}, "
                           f"占比:{stat['percentage']}%, 出现:{stat['count']}次")
        
        # 打印完整响应(仅在调试模式)
        if app.debug:
            logger.debug(f"完整响应: {json.dumps(result, ensure_ascii=False)[:1000]}...")
            
        return jsonify(result)
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"批量分析失败 ({process_time:.2f}秒): {str(e)}", exc_info=True)
        return jsonify({"error": f"分析失败: {str(e)}"}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    logger.info("收到健康检查请求")
    return jsonify({"status": "healthy", "model_loaded": True})

# 添加请求前后的日志
@app.before_request
def before_request():
    request.start_time = time.time()
    logger.info(f"接收请求: {request.method} {request.path}")

@app.after_request
def after_request(response):
    if hasattr(request, 'start_time'):
        process_time = time.time() - request.start_time
        logger.info(f"请求完成: {request.method} {request.path} - 状态:{response.status_code}, 耗时:{process_time:.2f}秒")
    return response

if __name__ == '__main__':
    logger.info("启动服务器: 监听 0.0.0.0:5001")
    app.run(debug=False, host='0.0.0.0', port=5001)