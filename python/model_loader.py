import os
from models.aspect_model import AspectSentimentModel
from config import logger

# 加载训练好的模型
def load_model():
    model_path = "saved_models/aspect_model_best"  # 验证集上表现最好的模型
    if not os.path.exists(model_path):
        model_path = "saved_models/aspect_model"  # 备选常规模型

    logger.info(f"正在加载模型: {model_path}")
    model = AspectSentimentModel(load_path=model_path)
    logger.info("模型加载完成！")
    return model