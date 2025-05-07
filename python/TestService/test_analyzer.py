import unittest
import logging
import numpy as np
from analyzer import (
    extract_key_phrases, analyze_reviews_batch, analyze_phrase_sentiment,
    preprocess_reviews, create_enhanced_vectors, precision_clustering
)
import jieba
import jieba.posseg as pseg
import re

class TestAnalyzer(unittest.TestCase):
    """全面测试评论分析系统的各个组件"""
    
    def setUp(self):
        """测试前准备工作"""
        # 示例评论集，覆盖多种表达方式和场景
        self.sample_reviews = [
            "感觉服务态度不错，但是量比较小，味道还行",
            "环境很好，服务员态度也很好，就是价格有点贵",
            "菜品味道一般，不值这个价，不推荐",
            "位置很好找，停车方便，厨师手艺很棒！",
            "分量足，价格实惠，非常满意，下次还会再来",
            "服务太差了，等了半小时才上菜，而且服务员态度冷淡",
            "食材新鲜度很高，特别是海鲜，值得推荐",
            "口味有特色，环境干净整洁，服务热情"
        ]
    
    def test_phrase_extraction_basic(self):
        """测试基本短语提取功能"""
        for i, review in enumerate(self.sample_reviews):
            phrases = extract_key_phrases(review)
            print(f"\n测试评论 {i+1}: {review}")
            print(f"提取短语: {phrases}")
            self.assertTrue(len(phrases) > 0, f"评论{i+1}应提取出至少一个短语")
    
    def test_phrase_extraction_pattern_types(self):
        """测试不同类型表达方式的短语提取"""
        test_cases = [
            # [评论, 期望能提取的短语部分内容, 描述]
            ["服务态度很好", "服务态度", "名词+形容词组合"],
            ["服务很周到", "服务很周到", "带程度词的表达"],
            ["这里的环境不错", "环境不错", "简单评价"],
            ["菜品非常新鲜", "菜品非常新鲜", "带强度词"],
            ["价格有点贵", "价格有点贵", "比较类表达"],
            ["味道不太好", "味道不太好", "否定表达"]
        ]
        
        for review, expected_part, desc in test_cases:
            phrases = extract_key_phrases(review)
            print(f"\n测试「{desc}」: {review}")
            print(f"提取短语: {phrases}")
            
            # 检查是否至少有一个短语包含期望部分
            match_found = any(expected_part in phrase for phrase in phrases)
            self.assertTrue(match_found, 
                           f"'{review}'中未找到包含'{expected_part}'的短语")
    
    def test_jieba_segmentation(self):
        """测试结巴分词和词性标注"""
        review = "服务态度很好，菜品新鲜美味"
        
        # 测试基本分词
        tokens = list(jieba.cut(review))
        print(f"\n基本分词结果: {tokens}")
        self.assertTrue(len(tokens) > 3, "基本分词应产生多个词")
        
        # 测试词性标注
        words = list(pseg.cut(review))
        print(f"词性标注结果: {words}")
        self.assertTrue(len(words) > 0, "词性标注应正常工作")
        
        # 检查是否有名词和形容词标记
        pos_tags = [pos for word, pos in words]
        self.assertTrue(any(tag.startswith('n') for tag in pos_tags), "应包含名词")
        self.assertTrue(any(tag.startswith('a') for tag in pos_tags), "应包含形容词")
    
    def test_enhanced_pattern_matching(self):
        """测试增强的正则表达式模式"""
        # 针对不同评论模式的测试用例
        test_patterns = [
            (r'(环境|服务|态度|味道)[^，。！？,.!?;；:：""''（）()]{0,8}(好|差|棒|糟|还行|不错)', 
             "环境很好，服务态度不错"),
            (r'(比较|感觉|有点)[^，。！？,.!?;；:：""''（）()]{1,10}(好|差|快|慢|贵|便宜)', 
             "感觉价格比较贵"),
            (r'(不|没)(算|太|很|是)[^，。！？,.!?;；:：""''（）()]{0,8}(好|差|满意)', 
             "味道不太好")
        ]
        
        for pattern, text in test_patterns:
            matches = re.findall(pattern, text)
            print(f"\n测试模式 '{pattern}'")
            print(f"文本: '{text}'")
            print(f"匹配结果: {matches}")
            self.assertTrue(len(matches) > 0, f"模式应匹配文本'{text}'")
    
    def test_sentiment_analysis(self):
        """测试情感分析功能"""
        test_phrases = [
            "很好吃", "非常难吃", "服务态度差", 
            "环境不错", "性价比高", "质量一般"
        ]
        
        print("\n情感分析结果:")
        for phrase in test_phrases:
            sentiment, score = analyze_phrase_sentiment(phrase)
            print(f"'{phrase}' => {sentiment} (置信度: {score:.2f})")
            self.assertIsNotNone(sentiment, f"'{phrase}'应有情感判断")
            self.assertIsInstance(score, float, "置信度应为浮点数")
            self.assertTrue(0 <= score <= 1, "置信度应在0-1之间")
    
    def test_vector_creation(self):
        """测试短语向量化功能"""
        phrases = ["服务态度好", "环境不错", "价格实惠"]
        vectors, segmented = create_enhanced_vectors(phrases)
        
        print(f"\n向量化结果: 形状{vectors.shape}")
        print(f"分词结果: {segmented}")
        
        self.assertEqual(len(vectors), len(phrases), "向量数应等于短语数")
        self.assertTrue(vectors.shape[1] > 0, "向量维度应大于0")

    def test_clustering(self):
        """测试聚类功能"""
        # 创建一些测试向量
        test_vectors = np.array([
            [1.0, 0.2, 0.1],  # 组1
            [0.9, 0.3, 0.2],  # 组1
            [0.2, 0.8, 0.7],  # 组2
            [0.1, 0.9, 0.8],  # 组2
            [0.5, 0.5, 0.5],  # 独立点
        ])
        phrases = ["好吃", "美味", "环境好", "环境不错", "一般"]

        clusters = precision_clustering(phrases, test_vectors)
        print(f"\n聚类结果: {clusters}")

        # 新的断言方式：检查相似向量分在同一组
        same_cluster_found = False

        for cluster_id, indices in clusters.items():
            # 检查任何一组中是否同时包含索引0和1 (好吃和美味)
            if 0 in indices and 1 in indices:
                same_cluster_found = True
                break

        self.assertTrue(same_cluster_found, "相似向量'好吃'和'美味'应分到同组")
    
    def test_complete_pipeline(self):
        """测试完整分析流程"""
        # 使用多条评论的列表
        reviews = self.sample_reviews[:3]  # 取前3条进行测试
        
        try:
            # 配置日志以显示详细信息
            logging.basicConfig(level=logging.INFO)
            
            print("\n开始测试完整分析流程...")
            result = analyze_reviews_batch(reviews)
            
            print(f"生成摘要: {result['summary']}")
            print(f"识别短语数: {len(result['phrase_stats'])}")
            for i, phrase in enumerate(result['phrase_stats'][:3]):
                print(f"Top {i+1}: {phrase['phrase']} ({phrase['sentiment']}, {phrase['percentage']}%)")
            
            # 验证结果
            self.assertIn('summary', result, "应包含摘要")
            self.assertIn('phrase_stats', result, "应包含短语统计")
            self.assertIn('total_reviews', result, "应包含评论总数")
            self.assertEqual(result['total_reviews'], len(reviews), "评论总数应正确")
            
            if result['phrase_stats']:
                self.assertTrue(all('phrase' in p and 'sentiment' in p for p in result['phrase_stats']), 
                              "每个统计项应包含短语和情感")
                
        except Exception as e:
            self.fail(f"完整流程测试失败: {str(e)}")

if __name__ == "__main__":
    unittest.main()