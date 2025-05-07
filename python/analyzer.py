import jieba
from sklearn.cluster import DBSCAN

from text_utils import split_into_sentences, clean_text
from model_loader import load_model
from config import logger
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer
import requests
import jieba.posseg as pseg
import re
from transformers import pipeline
import numpy as np
from gensim.models import Word2Vec
from sklearn.feature_extraction.text import TfidfVectorizer


# 加载情感分析模型
sentiment_analyzer = pipeline(
    "text-classification", 
    model="uer/roberta-base-finetuned-jd-binary-chinese",
    return_all_scores=True
)

def analyze_phrase_sentiment(phrase):
    """对短语进行情感分析"""
    try:
        result = sentiment_analyzer(phrase)
        # 模型输出正面情感的得分
        positive_score = next(score['score'] for score in result[0] if score['label'] == 'positive')
        if positive_score > 0.6:
            return "好", positive_score
        elif positive_score < 0.4:
            return "差", 1 - positive_score
        else:
            return "中性", max(positive_score, 1 - positive_score)
    except Exception as e:
        logger.error(f"情感分析出错: {str(e)}")
        return "未知", 0.5

def generate_summary_with_ollama(clustered_phrases, total_reviews):
    """通过Ollama模型生成增强摘要"""
    # 计算总评论数
    total_count = total_reviews
    
    # 按情感分类聚合数据
    positive_phrases = []
    negative_phrases = []
    neutral_phrases = []
    
    for cluster in clustered_phrases:
        phrase = cluster['representative']
        count = cluster['count']
        sentiment, confidence = analyze_phrase_sentiment(phrase)
        
        percentage = round((count / total_count) * 100, 1)
        phrase_info = f"{phrase} ({percentage}%, {count}条评论)"
        
        if sentiment == "好":
            positive_phrases.append(phrase_info)
        elif sentiment == "差":
            negative_phrases.append(phrase_info)
        else:
            neutral_phrases.append(phrase_info)
    
    # 构建提示词
    prompt = "请根据以下评论数据生成一段全面客观的餐厅评价摘要。\n\n"
    prompt += f"总评论数: {total_count}条\n\n"
    
    if positive_phrases:
        prompt += "正面评价:\n"
        for phrase in positive_phrases[:5]:  # 限制数量避免过长
            prompt += f"- {phrase}\n"
        prompt += "\n"
        
    if negative_phrases:
        prompt += "负面评价:\n"
        for phrase in negative_phrases[:5]:
            prompt += f"- {phrase}\n"
        prompt += "\n"
        
    if neutral_phrases:
        prompt += "中性评价:\n"
        for phrase in neutral_phrases[:3]:
            prompt += f"- {phrase}\n"
        prompt += "\n"
    
    prompt += "请生成一段300字以内的摘要，突出主要优点和不足，风格简洁客观。根据数据比例合理分配正面和负面内容的篇幅。"
    
    # 调用Ollama模型
    response = requests.post(
        url="http://localhost:11434/api/generate",
        json={
            "model": "llama3.2:latest",
            "prompt": prompt,
            "temperature": 0.5,  # 降低温度以获得更确定的输出
            "max_tokens": 600
        }
    )
    
    if response.status_code == 200:
        return response.json().get("response", "无法生成摘要")
    else:
        logger.error(f"Ollama调用失败: {response.text}")
        return "摘要生成失败"

model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')  # 加载语义嵌入模型



def preprocess_reviews(reviews):
    """使用高精度聚类进行评论预处理和聚类"""
    # 清洗评论文本
    cleaned_reviews = [clean_text(review) for review in reviews]
    
    # 从每条评论中提取关键短语
    all_phrases = []
    phrase_to_review_map = {}  # 记录短语来源的评论
    
    for i, review in enumerate(cleaned_reviews):
        phrases = extract_key_phrases(review)
        for phrase in phrases:
            all_phrases.append(phrase)
            if phrase not in phrase_to_review_map:
                phrase_to_review_map[phrase] = []
            phrase_to_review_map[phrase].append(i)
    
    # 对短语进行向量嵌入
    if not all_phrases:
        logger.warning("未从评论中提取到有效短语")
        return []
        
    # 使用增强词向量技术
    embeddings, segmented_phrases = create_enhanced_vectors(all_phrases)
    
    # 使用高精度聚类算法
    cluster_mapping = precision_clustering(all_phrases, embeddings)
    
    # 构建聚类结果
    clustered_phrases = []
    
    for label, indices in cluster_mapping.items():
        if label == -1:  # 跳过噪声点
            continue
            
        # 选择代表性短语：出现频率最高的短语
        phrase_counts = {}
        all_reviews = set()
        cluster_phrases = []
        
        for idx in indices:
            phrase = all_phrases[idx]
            cluster_phrases.append(phrase)
            count = len(phrase_to_review_map[phrase])
            phrase_counts[phrase] = count
            all_reviews.update(phrase_to_review_map[phrase])
            
        # 选择频率最高的短语作为代表
        representative = max(phrase_counts.items(), key=lambda x: x[1])[0]
        
        clustered_phrases.append({
            'representative': representative,
            'phrases': cluster_phrases,
            'count': len(all_reviews)
        })
    
    # 处理剩余的噪声点（单独成聚类）
    if -1 in cluster_mapping:
        for idx in cluster_mapping[-1]:
            phrase = all_phrases[idx]
            clustered_phrases.append({
                'representative': phrase,
                'phrases': [phrase],
                'count': len(phrase_to_review_map[phrase])
            })
    
    # 按出现次数降序排序
    clustered_phrases.sort(key=lambda x: x['count'], reverse=True)
    
    return clustered_phrases

model = load_model()

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
    """批量分析多条评论，生成高质量摘要"""
    try:
        # 1. 使用增强的预处理和聚类
        clustered_phrases = preprocess_reviews(reviews)
        
        if not clustered_phrases:
            return {
                'summary': "未能从评论中提取有效内容",
                'phrase_stats': [],
                'total_reviews': len(reviews)
            }
        
        # 2. 增加情感分析
        phrase_stats = []
        for cluster in clustered_phrases:
            phrase = cluster['representative']
            count = cluster['count']
            sentiment, confidence = analyze_phrase_sentiment(phrase)
            
            phrase_stats.append({
                'phrase': phrase,
                'count': count,
                'percentage': round((count / len(reviews)) * 100, 1),
                'sentiment': sentiment,
                'confidence': round(confidence, 2),
                'similar_phrases': cluster['phrases'][:5]  # 限制显示数量
            })
        
        # 3. 生成高质量摘要
        summary = generate_summary_with_ollama(clustered_phrases, len(reviews))
        
        return {
            'summary': summary,
            'phrase_stats': phrase_stats,
            'total_reviews': len(reviews)
        }
    
    except Exception as e:
        logger.error(f"批量分析过程中发生错误: {str(e)}", exc_info=True)
        return {
            'summary': f"分析过程中发生错误: {str(e)}",
            'phrase_stats': [],
            'total_reviews': len(reviews)
        }


def create_enhanced_vectors(phrases):
    """创建增强的短语向量表示"""
    # 加载餐饮领域词典（如果文件存在）
    try:
        jieba.load_userdict("restaurant_terms.txt")
    except:
        logger.warning("餐饮领域词典未找到，使用默认词典")
    
    # 分词处理
    segmented = [list(jieba.cut(phrase)) for phrase in phrases]
    
    # 训练领域特定词向量
    if len(segmented) < 5:  # 数据太少，使用默认模型
        logger.info("短语数量不足，使用 SentenceTransformer")
        return model.encode(phrases), segmented
    
    # 训练 Word2Vec 模型
    w2v_model = Word2Vec(segmented, vector_size=100, min_count=1, workers=4)
    
    # 结合 TF-IDF 权重
    tfidf = TfidfVectorizer().fit(["".join(sent) for sent in segmented])
    word_weights = {word: tfidf.vocabulary_.get(word, 0.1) 
                   for word in w2v_model.wv.key_to_index}
    
    # 生成短语向量
    phrase_vectors = []
    for tokens in segmented:
        if not tokens:
            phrase_vectors.append(np.zeros(100))
            continue
        
        vectors = []
        weights = []
        for word in tokens:
            if word in w2v_model.wv:
                vectors.append(w2v_model.wv[word])
                weights.append(word_weights.get(word, 0.1))
                
        if vectors:
            avg_vector = np.average(vectors, axis=0, weights=weights)
            phrase_vectors.append(avg_vector)
        else:
            phrase_vectors.append(np.zeros(100))
            
    return np.array(phrase_vectors), segmented

def extract_key_phrases(review):
    """从评论中提取关键短语，优化版"""
    # 使用jieba进行词性标注
    words = pseg.cut(review)
    
    # 提取词和词性
    word_pos_list = [(word, flag) for word, flag in words]
    
    # 构建有意义的短语
    phrases = []
    i = 0
    while i < len(word_pos_list):
        word, flag = word_pos_list[i]
        
        # 处理方面词 + 情感词的组合
        if flag.startswith('n') and i + 2 < len(word_pos_list):
            if word_pos_list[i+1][1].startswith('a') or word_pos_list[i+1][1].startswith('d'):
                if word_pos_list[i+2][1].startswith('a'):
                    # 形式如"服务态度很好"
                    phrases.append(word + word_pos_list[i+1][0] + word_pos_list[i+2][0])
                    i += 3
                    continue
                
        # 处理"很好"、"非常棒"等情感表达
        if (flag.startswith('d') or flag.startswith('a')) and i + 1 < len(word_pos_list):
            if word_pos_list[i+1][1].startswith('a'):
                phrases.append(word + word_pos_list[i+1][0])
                i += 2
                continue
                
        # 处理单独的方面词（至少两个字的名词）
        if flag.startswith('n') and len(word) >= 2:
            phrases.append(word)
            
        i += 1
    
    # 使用正则表达式提取特定模式的短语
    patterns = [
        r'(环境|服务|味道|价格|位置|速度|分量|新鲜度)(很|非常|特别|极其|太)?(好|差|棒|糟|赞|烂|香)(了)?',  # 评价短语
        r'(等|上菜)[^，。！？,.!?;；:：""''（）()]{0,5}(很|非常|特别)?(快|慢|久)(了)?'  # 速度相关评价
    ]
    
    for pattern in patterns:
        matches = re.findall(pattern, review)
        for match in matches:
            if isinstance(match, tuple):
                # 合并元组中的所有非空元素
                phrase = ''.join(part for part in match if part)
                if phrase:
                    phrases.append(phrase)
    
    # 过滤太短的短语
    filtered_phrases = [phrase for phrase in phrases if len(phrase) >= 2]
    
    # 去除重复短语
    return list(set(filtered_phrases))



#高精度聚类实现
def precision_clustering(phrases, vectors):
    """使用密度聚类算法进行高精度短语聚类"""
    # 计算相似度矩阵并确定自适应阈值
    similarity_matrix = cosine_similarity(vectors)
    eps = np.percentile(similarity_matrix.flatten(), 85)  # 自适应相似度阈值
    
    # 执行DBSCAN聚类
    clustering = DBSCAN(
        eps=eps,
        min_samples=2,  # 至少2个样本才形成聚类
        metric='precomputed'  # 使用预计算的距离矩阵
    ).fit(1 - similarity_matrix)  # 将相似度转换为距离
    
    labels = clustering.labels_
    n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
    logger.info(f"密度聚类找到 {n_clusters} 个聚类，噪声点数量: {list(labels).count(-1)}")
    
    # 处理噪声点(-1标签)
    reassign_outliers(similarity_matrix, labels)
    
    # 划分过大的聚类
    large_cluster_threshold = max(len(phrases) // 10, 5)  # 聚类大小阈值
    split_large_clusters(vectors, labels, large_cluster_threshold)
    
    # 构建聚类结果
    clusters = {}
    for i, label in enumerate(labels):
        if label not in clusters:
            clusters[label] = []
        clusters[label].append(i)
    
    return clusters

def reassign_outliers(similarity_matrix, labels):
    """将噪声点重新分配到最相似的聚类"""
    outlier_indices = [i for i, l in enumerate(labels) if l == -1]
    
    for i in outlier_indices:
        # 找出与当前噪声点最相似的非噪声样本
        similarities = [(j, similarity_matrix[i, j]) 
                       for j in range(len(labels)) if labels[j] != -1]
        
        if similarities:
            # 按相似度排序并取最相似的点
            similarities.sort(key=lambda x: x[1], reverse=True)
            best_match_idx, sim_score = similarities[0]
            
            # 如果相似度高于阈值，将噪声点分配给最相似点的聚类
            if sim_score > 0.65:  # 较低阈值，便于分配
                labels[i] = labels[best_match_idx]
                logger.debug(f"噪声点 {i} 被重新分配到聚类 {labels[best_match_idx]}")

def split_large_clusters(vectors, labels, threshold):
    """拆分过大的聚类"""
    # 计算各聚类大小
    cluster_sizes = Counter(labels)
    
    # 记录原始最大标签，以便分配新标签
    max_label = max(labels) if labels.size > 0 and max(labels) >= 0 else 0
    new_label = max_label
    
    for cluster_id, size in cluster_sizes.items():
        if cluster_id == -1 or size <= threshold:  # 跳过噪声点和小聚类
            continue
            
        # 找出属于该聚类的所有点
        cluster_indices = [i for i, l in enumerate(labels) if l == cluster_id]
        cluster_vectors = vectors[cluster_indices]
        
        # 对大聚类进行进一步聚类（使用更严格的参数）
        sub_clustering = DBSCAN(
            eps=0.2,  # 更严格的阈值
            min_samples=2,
            metric='cosine'
        ).fit(cluster_vectors)
        
        sub_labels = sub_clustering.labels_
        n_sub_clusters = len(set(sub_labels)) - (1 if -1 in sub_labels else 0)
        
        if n_sub_clusters > 1:  # 只有在成功分割的情况下才应用
            for i, sub_label in enumerate(sub_labels):
                if sub_label != -1:  # 保留为噪声点的样本
                    # 分配新的聚类标签
                    new_label += 1
                    labels[cluster_indices[i]] = new_label