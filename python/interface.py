import os
from models.aspect_model import AspectSentimentModel

def main():
    print("=" * 50)
    print("餐厅评论分析系统")
    print("=" * 50)

    # 加载训练好的模型
    model_path = "saved_models/aspect_model_best"  # 使用验证集上表现最好的模型
    if not os.path.exists(model_path):
        model_path = "saved_models/aspect_model"  # 如果没有best模型，使用最终模型

    print(f"正在加载模型: {model_path}")
    model = AspectSentimentModel(load_path=model_path)
    print("模型加载完成！")

    # 准备一些测试评论
    test_reviews = [
        "这家餐厅环境非常好，装修很有格调，但是服务态度一般，上菜有点慢。",
        "菜品味道还不错，尤其是招牌菜很美味，价格也比较实惠。",
        "位置很好找，交通便利，就是价格有点小贵。",
        "环境一般，服务挺热情的，菜品种类多，味道也不错，性价比很高。"
    ]

    # 分析评论
    print("\n=== 评论分析结果 ===\n")

    for review in test_reviews:
        print(f"评论: {review}")
        results = model.predict(review)

        if results:
            print("分析结果:")
            for result in results:
                print(f"  • {result['text']} -> {result['aspect']}:{result['sentiment']} (置信度: {result['confidence']:.2f})")
        else:
            print("未检测到明确的方面和情感")
        print("-" * 30)

    # 交互式评论分析
    print("\n=== 交互式评论分析 ===")
    print("输入餐厅评论进行分析 (输入'退出'结束程序)")

    while True:
        user_input = input("\n请输入餐厅评论: ")
        if user_input.lower() in ['退出', 'exit', 'quit']:
            break

        results = model.predict(user_input)
        if results:
            print("分析结果:")
            for result in results:
                print(f"  • {result['text']} -> {result['aspect']}:{result['sentiment']} (置信度: {result['confidence']:.2f})")
        else:
            print("未检测到明确的方面和情感")

if __name__ == "__main__":
    main()