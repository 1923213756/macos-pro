import logging
import torch

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

# 检查设备
device = torch.device("mps" if torch.backends.mps.is_available() else
                      "cuda" if torch.cuda.is_available() else "cpu")
logger.info(f"使用设备: {device}")