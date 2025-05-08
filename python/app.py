from flask import Flask, request, jsonify
from config import logger
from analyzer import analyze_reviews_batch,create_enhanced_vectors, cosine_similarity,extract_key_phrases
import time
import json

app = Flask(__name__)

@app.route('/api/match_faq', methods=['POST'])
def match_faq():
    data = request.json
    question = data.get('question', '')
    existing_faqs = data.get('existing_faqs', [])
    
    try:
        # 1. 提取用户问题的关键短语（利用现有function）
        question_phrases = extract_key_phrases(question)
        
        # 2. 为用户问题和FAQ创建增强向量（利用现有function）
        all_texts = question_phrases + [faq['question'] for faq in existing_faqs]
        vectors, _ = create_enhanced_vectors(all_texts)
        
        # 3. 使用相似度计算，类似于reassign_outliers的思路
        question_vectors = vectors[:len(question_phrases)]
        faq_vectors = vectors[len(question_phrases):]
        
        # 计算问题与FAQ的最大相似度
        best_matches = []
        for i, faq in enumerate(existing_faqs):
            max_sim = max([cosine_similarity([qv], [faq_vectors[i]])[0][0] 
                          for qv in question_vectors])
            if max_sim > 0.6:
                best_matches.append({
                    'faq_id': faq['id'],
                    'question': faq['question'],
                    'answer': faq['answer'],
                    'similarity': float(max_sim)
                })
        
        # 排序返回结果
        best_matches.sort(key=lambda x: x['similarity'], reverse=True)
        
        return jsonify({
            'matched_faqs': best_matches[:3],
            'original_question': question
        })
        
    except Exception as e:
        logger.error(f"FAQ匹配失败: {str(e)}", exc_info=True)
        return jsonify({"error": str(e), "matched_faqs": []}), 500

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

def generate_question_from_phrase(phrase, sentiment):
    """从短语生成可能的FAQ问题"""
    try:
        # 调用Ollama模型生成问题
        prompt = f"""
        请将以下短语转换为餐厅FAQ中的一个问题。
        短语: "{phrase}"
        情感: {sentiment}
        
        要求:
        1. 问题要简短明了
        2. 以"您好，请问"或"请问"开头
        3. 针对餐厅常见咨询场景
        4. 不要在问题中包含答案
        
        仅返回一个问题，不要有其他内容:
        """
        
        response = requests.post(
            url="http://localhost:11434/api/generate",
            json={
                "model": "llama3.2:latest",
                "prompt": prompt,
                "temperature": 0.7,
                "max_tokens": 100,
                "stream": False
            },
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            question = result.get("response", "").strip()
            return question
        else:
            logger.warning(f"问题生成API调用失败: {response.status_code}")
            
    except Exception as e:
        logger.error(f"生成问题失败: {str(e)}")
    
    # 如果调用失败，使用模板生成简单问题
    if sentiment == "好":
        return f"请问贵餐厅的{phrase}怎么样？"
    elif sentiment == "差":
        return f"我听说你们餐厅{phrase}有问题，是真的吗？"
    else:
        return f"关于贵餐厅的{phrase}，能详细介绍一下吗？"

def generate_answer_from_phrase(phrase, sentiment, count, total_reviews):
    """从短语生成可能的FAQ答案"""
    try:
        # 计算百分比
        percentage = round((count / total_reviews) * 100)
        
        # 调用Ollama模型生成答案
        prompt = f"""
        请为餐厅FAQ生成一个回答。
        
        关键信息:
        - 关于: "{phrase}" 
        - 评价情感: {sentiment}
        - 约 {percentage}% 的顾客提到了这个方面
        
        要求:
        1. 回答应简洁、真诚且有帮助
        2. 如果情感为"好"，可以适当强调这是餐厅的优点
        3. 如果情感为"差"，应以诚恳态度回应并说明改进措施
        4. 回答长度控制在100字以内
        
        仅返回一个回答，不要有其他内容:
        """
        
        response = requests.post(
            url="http://localhost:11434/api/generate",
            json={
                "model": "llama3.2:latest",
                "prompt": prompt,
                "temperature": 0.7,
                "max_tokens": 200,
                "stream": False
            },
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            answer = result.get("response", "").strip()
            return answer
        else:
            logger.warning(f"答案生成API调用失败: {response.status_code}")
            
    except Exception as e:
        logger.error(f"生成答案失败: {str(e)}")
    
    # 如果调用失败，使用模板生成简单答案
    if sentiment == "好":
        return f"感谢您的关注！我们的{phrase}确实得到了许多顾客的好评。我们一直致力于提供最优质的服务，欢迎您来体验！"
    elif sentiment == "差":
        return f"非常感谢您的反馈。关于{phrase}的问题，我们已经注意到并正在积极改进中。我们重视每位顾客的体验，期待为您提供更好的服务。"
    else:
        return f"关于{phrase}，我们餐厅一直在努力做到最好。欢迎您前来品尝，也欢迎提出宝贵建议，帮助我们不断进步。"

@app.route('/api/cluster', methods=['POST'])
def cluster_reviews_for_faq():
    """API端点：聚类评论并生成FAQ建议"""
    start_time = time.time()
    data = request.json
    reviews = data.get('reviews', [])
    
    # 记录请求详情
    logger.info(f"收到FAQ生成请求 - {len(reviews)} 条评论")
    
    try:
        # 1. 使用评论分析功能提取关键短语
        logger.info("开始评论聚类用于FAQ生成...")
        result = analyze_reviews_batch(reviews)
        phrase_stats = result.get('phrase_stats', [])
        
        if not phrase_stats:
            logger.warning("没有提取到有效的短语统计，无法生成FAQ")
            return jsonify({"suggested_faqs": [], "message": "无法从评论中提取有效短语"})
        
        # 2. 根据短语生成FAQ（问题和答案）
        logger.info(f"从 {len(phrase_stats)} 个提取的短语统计中生成FAQ")
        suggested_faqs = []
        
        # 仅使用前10个最重要的短语生成FAQ
        for stat in phrase_stats[:10]:
            phrase = stat['phrase']
            sentiment = stat['sentiment']
            count = stat['count']
            percentage = stat['percentage']
            
            # 生成问题
            question = generate_question_from_phrase(phrase, sentiment)
            
            # 生成回答
            answer = generate_answer_from_phrase(phrase, sentiment, count, len(reviews))
            
            # 记录生成的FAQ
            logger.info(f"生成FAQ - 问题: {question}")
            logger.info(f"生成FAQ - 答案: {answer[:50]}...")
            
            # 添加到结果列表
            suggested_faqs.append({
                "question": question,
                "answer": answer
            })
        
        # 记录处理结果
        total_time = time.time() - start_time
        logger.info(f"FAQ生成完成! 处理时间: {total_time:.2f}秒, 生成 {len(suggested_faqs)} 个FAQ建议")
        
        return jsonify({
            "suggested_faqs": suggested_faqs,
            "processing_time": total_time,
            "total_reviews": len(reviews)
        })
        
    except Exception as e:
        logger.error(f"FAQ生成失败: {str(e)}", exc_info=True)
        return jsonify({"error": f"FAQ生成失败: {str(e)}", "suggested_faqs": []}), 500

if __name__ == '__main__':
    logger.info("启动服务器: 监听 0.0.0.0:5001")
    app.run(debug=False, host='0.0.0.0', port=5001)