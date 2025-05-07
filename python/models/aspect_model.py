import os
import torch
import json
from transformers import AutoTokenizer, AutoModelForSequenceClassification, AutoConfig
from torch.optim import AdamW
from torch.utils.data import TensorDataset, DataLoader

class AspectSentimentModel:
    # 类变量存储共享实例
    _instance = None
    _is_initialized = False
    
    @classmethod
    def get_instance(cls, model_name="hfl/chinese-roberta-wwm-ext-large", load_path=None):
        """单例模式获取模型实例，避免重复加载"""
        if cls._instance is None:
            cls._instance = cls(model_name, load_path, _initialize=False)
        return cls._instance
    
    def __init__(self, model_name="hfl/chinese-roberta-wwm-ext-large", load_path=None, _initialize=True):
        # 基础属性设置，不加载模型
        self.model_name = model_name
        self.load_path = load_path
        self.aspects = ["环境", "服务", "口味", "价格", "位置", "速度", "分量", "新鲜度"]
        self.sentiments = ["好", "差"]
        
        # 只有在显式要求时才初始化模型
        if _initialize:
            self._initialize_model()
    
    def _initialize_model(self):
        """延迟初始化模型 - 仅在需要时调用"""
        if hasattr(self, 'model') and self.model is not None:
            # 已经初始化过，不重复加载
            return
            
        print("开始加载方面情感分析模型...")
        self.device = torch.device("cuda" if torch.cuda.is_available() else
                               "mps" if torch.backends.mps.is_available() else "cpu")
        print(f"使用设备: {self.device}")
        
        # 以下是原有的模型加载代码
        if self.load_path:
            try:
                # 首先尝试从模型配置中加载自定义方面列表
                try:
                    config_path = os.path.join(self.load_path, "config.json")
                    if os.path.exists(config_path):
                        with open(config_path, "r", encoding="utf-8") as f:
                            config = json.load(f)
                            if "aspects" in config:
                                self.aspects = config["aspects"]
                                print(f"从模型配置加载了方面列表: {self.aspects}")
                except Exception as e:
                    print(f"加载方面列表失败，将使用默认方面列表: {str(e)}")

                # 加载模型
                print(f"正在加载已训练的模型: {self.load_path}")
                self.tokenizer = AutoTokenizer.from_pretrained(self.load_path)
                
                # 尝试加载模型
                try:
                    self.model = AutoModelForSequenceClassification.from_pretrained(
                        self.load_path,
                        ignore_mismatched_sizes=True
                    ).to(self.device)
                    print("模型加载成功(使用ignore_mismatched_sizes)！")
                except Exception as e1:
                    # 备用加载方式...
                    print(f"第一种加载方法失败: {str(e1)}")
                    # 其余代码保持不变...
                    
            except Exception as e:
                print(f"警告: 模型加载失败，但将继续运行: {str(e)}")
                self.model = None
                self.tokenizer = None
        else:
            print("未指定模型路径，模型将在需要时加载")
            self.model = None
            self.tokenizer = None
    
    def train(self, train_data, epochs=5, batch_size=16, learning_rate=2e-5, save_path=None, eval_data=None):
        """训练方面情感分析模型并保存"""
        # 确保模型已初始化
        self._initialize_model()
        # 原有训练代码...
    
    def predict(self, text, threshold=0.1):
        """预测文本的方面和情感"""
        # 确保模型已初始化
        self._initialize_model()
        if self.model is None:
            return []  # 如果模型未能加载，返回空结果
        # 原有预测代码...