from flask import Flask, request, jsonify
from config import logger
from analyzer import  analyze_reviews_batch

app = Flask(__name__)



@app.route('/analyze_batch', methods=['POST'])
def analyze_batch():
    """API端点：批量分析多条评论，提供汇总统计"""
    data = request.json
    reviews = data.get('reviews', [])

    logger.info(f"收到 {len(reviews)} 条评论需要分析")

    try:
        result = analyze_reviews_batch(reviews)
        return jsonify(result)
    except Exception as e:
        logger.error(f"批量分析过程中发生错误: {str(e)}", exc_info=True)
        return jsonify({"error": f"分析失败: {str(e)}"}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({"status": "healthy", "model_loaded": True})

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)