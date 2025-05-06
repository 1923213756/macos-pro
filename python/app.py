import re
import os
import time
import logging
import torch
from flask import Flask, request, jsonify
from models.aspect_model import AspectSentimentModel

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("python_service.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("aspect_sentiment_service")

app = Flask(__name__)

# 记录服务启动信息
logger.info("======= 情感分析服务启动 =======")

# 检查设备
device = torch.device("mps" if torch.backends.mps.is_available() else
                      "cuda" if torch.cuda.is_available() else "cpu")
logger.info(f"使用设备: {device}")

app = Flask(__name__)

# 检查设备
device = torch.device("mps" if torch.backends.mps.is_available() else
                      "cuda" if torch.cuda.is_available() else "cpu")
print(f"使用设备: {device}")

# 加载训练好的模型
model_path = "saved_models/aspect_model_best"  # 验证集上表现最好的模型
if not os.path.exists(model_path):
    model_path = "saved_models/aspect_model"  # 备选常规模型

print(f"正在加载模型: {model_path}")
model = AspectSentimentModel(load_path=model_path)
print("模型加载完成！")

def split_into_sentences(text):
    """将文本分割成句子，使用更全面的标点符号模式"""
    # 使用常见的中文和英文标点进行分割
    pattern = r'([。！？?!;；,，\.…]+)'
    parts = re.split(pattern, text)

    # 合并标点符号与前面的文本
    sentences = []
    for i in range(0, len(parts)-1, 2):
        if parts[i].strip():
            sentences.append((parts[i] + parts[i+1]).strip())

    # 处理最后一个部分（如果存在且不为空）
    if len(parts) % 2 == 1 and parts[-1].strip():
        sentences.append(parts[-1].strip())

    # 如果没有获得有效句子，将整个文本作为一个句子
    if not sentences and text.strip():
        sentences = [text.strip()]

    return sentences

def clean_text(text):
    """清理文本，保留语义重要的内容"""
    # 移除HTML标签
    text = re.sub(r'<[^>]+>', '', text)
    # 移除表情符号和特殊字符，但保留标点和中英文
    text = re.sub(r'[^\u4e00-\u9fa5a-zA-Z0-9，。！？,.!?;；:：""''（）()]', ' ', text)
    # 移除多余空格
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def analyze_review(text):
    """分析单条评论，返回结构化结果"""
    # 清理文本
    clean_review = clean_text(text)

    # 分句处理
    sentences = split_into_sentences(clean_review)

    # 存储分析结果
    all_results = []

    # 对每个句子单独进行分析
    for sentence in sentences:
        if len(sentence) < 2:  # 忽略过短的句子
            continue

        # 调用模型进行分析
        results = model.predict(sentence)

        # 将分析结果与原句关联
        for result in results:
            result['text'] = sentence
            all_results.append(result)

    # 按方面对结果进行分组和去重（每个方面只保留最高置信度的情感）
    aspect_best_results = {}
    for result in all_results:
        aspect = result['aspect']
        if aspect not in aspect_best_results or result['confidence'] > aspect_best_results[aspect]['confidence']:
            aspect_best_results[aspect] = result

    # 将去重后的结果转换为列表
    unique_results = list(aspect_best_results.values())

    # 按照原始方面分组（用于展示详细结果）
    aspects_detailed = {}
    for result in all_results:
        aspect = result['aspect']
        if aspect not in aspects_detailed:
            aspects_detailed[aspect] = []
        aspects_detailed[aspect].append(result)

    # 生成摘要
    summary = generate_summary(aspect_best_results)

    return {
        'original_text': text,
        'sentences': sentences,
        'all_results': all_results,                  # 所有句子分析结果
        'unique_aspect_results': unique_results,     # 每个方面只保留最高置信度结果
        'aspects_detailed': aspects_detailed,        # 按方面分组的详细结果
        'summary': summary                           # 摘要文本
    }

def generate_summary(aspect_best_results):
    """根据去重后的方面情感结果生成摘要"""
    if not aspect_best_results:
        return "未检测到明确的方面和情感"

    summary_points = []
    for aspect, result in aspect_best_results.items():
        sentiment = result["sentiment"]
        if sentiment == "好":
            summary_points.append(f"{aspect}评价积极")
        else:
            summary_points.append(f"{aspect}评价消极")

    return "，".join(summary_points)

def analyze_reviews_batch(reviews):
    """批量分析多条评论，并统计方面情感计数"""
    # 存储每条评论的分析结果
    individual_results = []

    # 存储所有评论中每个方面-情感对的数量
    aspect_sentiment_counts = {}  # 格式: {"环境-好": 3, "服务-差": 2, ...}

    # 进行分析并统计
    for review in reviews:
        result = analyze_review(review)
        individual_results.append({
            'review': review,
            'analysis': result
        })

        # 统计每个评论中的唯一方面-情感对
        for aspect_result in result['unique_aspect_results']:
            aspect = aspect_result['aspect']
            sentiment = aspect_result['sentiment']
            key = f"{aspect}-{sentiment}"
            aspect_sentiment_counts[key] = aspect_sentiment_counts.get(key, 0) + 1

    # 转换统计结果为更友好的格式
    aspect_stats = {}
    for key, count in aspect_sentiment_counts.items():
        aspect, sentiment = key.split('-')
        if aspect not in aspect_stats:
            aspect_stats[aspect] = {'好': 0, '差': 0, '总计': 0}
        aspect_stats[aspect][sentiment] = count
        aspect_stats[aspect]['总计'] += count

    return {
        'individual_results': individual_results,
        'aspect_stats': aspect_stats
    }

@app.route('/analyze', methods=['POST'])
def analyze():
    """API端点：分析单条评论"""
    start_time = time.time()
    
    client_ip = request.remote_addr
    logger.info(f"收到单条评论分析请求 [来源IP: {client_ip}]")
    
    data = request.json
    if not data or 'text' not in data:
        logger.warning("请求缺少必要参数'text'")
        return jsonify({"error": "请提供评论文本"}), 400
    
    text = data['text']
    if len(text) > 50:
        log_text = text[:47] + "..."
    else:
        log_text = text
    logger.info(f"分析评论: {log_text}")
    
    try:
        result = analyze_review(text)
        
        # 记录分析结果概况
        aspect_count = len(result['unique_aspect_results']) if 'unique_aspect_results' in result else 0
        logger.info(f"成功提取 {aspect_count} 个方面的情感")
        
        # 记录处理时间
        process_time = time.time() - start_time
        logger.info(f"单条分析完成，处理时间: {process_time:.2f}秒")
        
        return jsonify(result)
    except Exception as e:
        logger.error(f"单条分析过程中发生错误: {str(e)}", exc_info=True)
        return jsonify({"error": f"分析失败: {str(e)}"}), 500



@app.route('/analyze_batch', methods=['POST'])
def analyze_batch():
    """API端点：批量分析多条评论，提供汇总统计"""
    start_time = time.time()
    
    # 记录请求信息
    client_ip = request.remote_addr
    logger.info(f"收到批量分析请求 [来源IP: {client_ip}]")
    
    data = request.json
    if not data or 'reviews' not in data:
        logger.warning("请求缺少必要参数'reviews'")
        return jsonify({"error": "请提供评论列表"}), 400
        
    reviews = data['reviews']
    review_count = len(reviews)
    logger.info(f"开始处理 {review_count} 条评论")
    
    # 记录一小部分评论示例（避免日志过大）
    if reviews:
        sample = reviews[0]
        if len(sample) > 50:
            sample = sample[:47] + "..."
        logger.info(f"评论样本: {sample}")
    
    # 分析评论
    try:
        results = analyze_reviews_batch(reviews)
        
        # 记录分析结果概况
        aspect_count = len(results['aspect_stats']) if 'aspect_stats' in results else 0
        logger.info(f"成功提取 {aspect_count} 个方面的情感")
        
        if 'aspect_stats' in results:
            for aspect, stats in results['aspect_stats'].items():
                logger.info(f"方面 '{aspect}': {stats['好']}好评, {stats['差']}差评, 总计{stats['总计']}条评论提及")
        
        # 记录处理时间
        process_time = time.time() - start_time
        logger.info(f"批量分析完成，处理时间: {process_time:.2f}秒")
        
        return jsonify(results)
    except Exception as e:
        logger.error(f"批量分析过程中发生错误: {str(e)}", exc_info=True)
        return jsonify({"error": f"分析失败: {str(e)}"}), 500

@app.route('/custom_summary', methods=['POST'])
def custom_summary():
    """API端点：自定义摘要格式"""
    data = request.json
    if not data or 'reviews' not in data:
        return jsonify({"error": "请提供评论列表"}), 400

    format_type = data.get('format', 'default')
    results = analyze_reviews_batch(data['reviews'])

    # 根据不同的格式生成不同的摘要
    if format_type == 'percentage':
        # 百分比格式摘要
        aspect_stats = results['aspect_stats']
        summary_points = []
        for aspect, stats in aspect_stats.items():
            total = stats['总计']
            if total > 0:
                positive_percentage = (stats['好'] / total) * 100
                summary_points.append(f"{aspect}: {positive_percentage:.1f}%好评")
        custom_summary = "，".join(summary_points)
    elif format_type == 'counts':
        # 计数格式摘要
        aspect_stats = results['aspect_stats']
        summary_points = []
        for aspect, stats in aspect_stats.items():
            summary_points.append(f"{aspect}: {stats['好']}好评/{stats['差']}差评")
        custom_summary = "，".join(summary_points)
    else:
        # 默认格式摘要
        aspect_stats = results['aspect_stats']
        summary_points = []
        for aspect, stats in aspect_stats.items():
            if stats['好'] > stats['差']:
                summary_points.append(f"{aspect}整体评价积极")
            elif stats['好'] < stats['差']:
                summary_points.append(f"{aspect}整体评价消极")
            else:
                summary_points.append(f"{aspect}评价不一")
        custom_summary = "，".join(summary_points)

    return jsonify({
        'aspect_stats': results['aspect_stats'],
        'custom_summary': custom_summary
    })

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({"status": "healthy", "model_loaded": True})

if __name__ == '__main__':
    # 测试代码
    test_reviews = [
        "环境很好，菜品味道也不错，但服务态度一般。菜品的环境氛围很棒。",
        "价格有点贵，但是菜品质量确实没得说，值这个价。环境也很优雅。",
        "位置不太好找，在一个角落里。服务倒是挺热情的，菜上得很快。"
    ]

    print("\n===== 测试单条评论分析 =====")
    for review in test_reviews:
        print(f"\n评论: {review}")
        result = analyze_review(review)

        print("每个方面的唯一结果 (去重后):")
        for res in result['unique_aspect_results']:
            print(f"  • {res['aspect']}:{res['sentiment']} - \"{res['text']}\" (置信度: {res['confidence']:.2f})")

        print(f"\n摘要: {result['summary']}")

    print("\n===== 测试批量评论分析 =====")
    batch_result = analyze_reviews_batch(test_reviews)

    print("\n方面情感统计:")
    for aspect, stats in batch_result['aspect_stats'].items():
        print(f"  • {aspect}: {stats['好']}好评, {stats['差']}差评, 总计{stats['总计']}条评论提及")

    # 启动Flask应用
    app.run(debug=True, host='0.0.0.0', port=5001)