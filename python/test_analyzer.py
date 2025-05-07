import unittest
from analyzer import extract_key_phrases, analyze_phrase_sentiment, preprocess_reviews, analyze_reviews_batch

class TestAnalyzer(unittest.TestCase):
    
    def test_extract_key_phrases(self):
        """测试关键短语提取功能"""
        review = "这家餐厅环境很好，服务态度特别棒，但是价格太贵了，味道一般。"
        phrases = extract_key_phrases(review)
        self.assertTrue(len(phrases) > 0, "应该提取出至少一个关键短语")
        print(f"提取的关键短语: {phrases}")
        
    def test_sentiment_analysis(self):
        """测试情感分析功能"""
        phrases = ["环境很好", "服务态度差", "价格太贵了", "味道非常棒"]
        for phrase in phrases:
            sentiment, confidence = analyze_phrase_sentiment(phrase)
            print(f"短语 '{phrase}' 的情感: {sentiment}, 置信度: {confidence}")
            self.assertIn(sentiment, ["好", "差", "中性", "未知"])
            
    def test_preprocess_reviews(self):
        """测试评论预处理和聚类功能"""
        reviews = [
            "这家餐厅环境很好，服务态度也不错",
            "餐厅环境非常好，推荐",
            "服务态度太差了，等了很久",
            "等了很长时间，服务太慢了"
        ]
        clusters = preprocess_reviews(reviews)
        self.assertTrue(len(clusters) > 0, "应该生成至少一个聚类")
        print(f"生成的聚类数量: {len(clusters)}")
        for i, cluster in enumerate(clusters):
            print(f"聚类 {i+1}: {cluster['representative']} (出现{cluster['count']}次)")
            
    def test_batch_analysis(self):
        """测试批量分析功能"""
        reviews = [
            "这家餐厅环境很好，服务态度也不错。",
            "餐厅环境非常好，推荐。",
            "服务态度太差了，等了很久。",
            "等了很长时间，服务太慢了。"
        ]
        result = analyze_reviews_batch(reviews)
        print(f"生成的摘要: {result['summary']}")
        print(f"短语统计数量: {len(result['phrase_stats'])}")
        
if __name__ == "__main__":
    unittest.main()