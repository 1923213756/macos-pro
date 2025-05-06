import os
import json
import random
import pandas as pd
from models.aspect_model import AspectSentimentModel

def prepare_aspect_data(aspects=None):
    """准备方面情感分析训练数据"""
    data = []

    # 加载原有数据
    try:
        with open("data/aspect_data.json", "r", encoding="utf-8") as f:
            data = json.load(f)
            print(f"已加载{len(data)}条数据")
    except FileNotFoundError:
        print("未找到方面情感分析训练数据文件")
        return [], []

    # 检查数据中的方面类别
    existing_aspects = set()
    for item in data:
        existing_aspects.add(item["aspect"])

    print(f"数据中存在的方面类别: {sorted(list(existing_aspects))}")

    # 如果提供了目标方面列表，检查是否需要补充数据
    if aspects:
        missing_aspects = [aspect for aspect in aspects if aspect not in existing_aspects]
        if missing_aspects:
            print(f"发现缺失的方面类别: {missing_aspects}")

            # 为缺失的方面生成样本数据（每个方面至少4个示例）
            supplementary_data = []
            examples = {
                "速度": [
                    {"text": "上菜非常快", "sentiment": "好"},
                    {"text": "点餐后很快就上来了", "sentiment": "好"},
                    {"text": "等了好久才上菜", "sentiment": "差"},
                    {"text": "上菜速度太慢了", "sentiment": "差"},
                ],
                "分量": [
                    {"text": "菜量很足", "sentiment": "好"},
                    {"text": "分量十足", "sentiment": "好"},
                    {"text": "分量太少了", "sentiment": "差"},
                    {"text": "菜的分量不足", "sentiment": "差"},
                ],
                "新鲜度": [
                    {"text": "食材很新鲜", "sentiment": "好"},
                    {"text": "海鲜特别新鲜", "sentiment": "好"},
                    {"text": "菜品不太新鲜", "sentiment": "差"},
                    {"text": "食材感觉不是当天的", "sentiment": "差"},
                ]
            }

            for aspect in missing_aspects:
                if aspect in examples:
                    for example in examples[aspect]:
                        supplementary_data.append({
                            "text": example["text"],
                            "aspect": aspect,
                            "sentiment": example["sentiment"]
                        })
                    print(f"为方面 '{aspect}' 添加了 {len(examples[aspect])} 条样本")

            # 合并数据
            if supplementary_data:
                data.extend(supplementary_data)
                print(f"数据集扩展至 {len(data)} 条")

    # 随机打乱数据
    random.shuffle(data)

    # 划分训练集和验证集
    split_idx = int(len(data) * 0.8)
    train_data = data[:split_idx]
    eval_data = data[split_idx:]

    # 检查训练集和验证集中各方面的样本数量
    train_aspects = {}
    eval_aspects = {}

    for item in train_data:
        aspect = item["aspect"]
        train_aspects[aspect] = train_aspects.get(aspect, 0) + 1

    for item in eval_data:
        aspect = item["aspect"]
        eval_aspects[aspect] = eval_aspects.get(aspect, 0) + 1

    print("\n训练集中各方面的样本数量:")
    for aspect, count in sorted(train_aspects.items()):
        print(f"  • {aspect}: {count}条")

    print("\n验证集中各方面的样本数量:")
    for aspect, count in sorted(eval_aspects.items()):
        print(f"  • {aspect}: {count}条")

    return train_data, eval_data

def train_aspect_model():
    """训练方面情感分析模型"""
    # 定义方面类别，包括扩展的方面
    aspects = ["环境", "服务", "口味", "价格", "位置", "速度", "分量", "新鲜度"]
    print(f"将训练包含以下方面的模型: {aspects}")

    # 确保模型保存目录存在
    os.makedirs("saved_models", exist_ok=True)

    # 准备包含所有方面的数据
    train_data, eval_data = prepare_aspect_data(aspects)

    if not train_data:
        print("没有找到方面情感分析训练数据，跳过训练")
        return False

    # 初始化模型 - 使用新的large模型
    model = AspectSentimentModel(
        model_name="hfl/chinese-roberta-wwm-ext-large",
        load_path=None  # 强制使用预训练模型
    )

    # 确保模型支持所有需要的方面
    model.aspects = aspects
    print(f"设置模型方面类别: {model.aspects}")

    # 训练模型 - 调整batch_size和learning_rate适应large模型
    print("\n开始训练模型...")
    model.train(
        train_data=train_data,
        eval_data=eval_data,
        epochs=5,
        batch_size=8,  # 减小batch_size，因为模型更大了
        learning_rate=1e-5,  # 降低学习率，large模型需要更平滑的学习
        save_path="saved_models/aspect_model"
    )

    # 测试几个示例，包括新方面
    test_examples = [
        "环境很好，菜品味道也不错",
        "服务太差了，态度极其恶劣",
        "性价比高，推荐尝试",
        "上菜速度很快",
        "菜量很足，但是食材不太新鲜"
    ]

    print("\n测试模型效果:")
    for example in test_examples:
        print(f"\n评论: {example}")
        results = model.predict(example)
        if results:
            for result in results:
                print(f"  • {result['aspect']}:{result['sentiment']} (置信度: {result['confidence']:.2f})")
        else:
            print("  • 未检测到明确的方面和情感")

    return True

def main():
    print("=" * 50)
    print("开始训练方面情感分析模型")
    print("=" * 50)

    try:
        aspect_trained = train_aspect_model()

        if aspect_trained:
            print("\n方面情感分析模型训练完成！模型已保存到 saved_models/aspect_model")
        else:
            print("\n方面情感分析模型训练失败或跳过")
    except Exception as e:
        print(f"\n训练过程中出错: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()