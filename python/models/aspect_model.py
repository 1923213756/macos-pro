import os
import torch
import json
from transformers import AutoTokenizer, AutoModelForSequenceClassification, AutoConfig
from torch.optim import AdamW
from torch.utils.data import TensorDataset, DataLoader

class AspectSentimentModel:
    def __init__(self, model_name="hfl/chinese-roberta-wwm-ext-large", load_path=None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else
                                   "mps" if torch.backends.mps.is_available() else "cpu")
        print(f"使用设备: {self.device}")

        # 扩展方面类别
        self.aspects = ["环境", "服务", "口味", "价格", "位置", "速度", "分量", "新鲜度"]
        self.sentiments = ["好", "差"]

        if load_path:
            try:
                # 首先尝试从模型配置中加载自定义方面列表
                try:
                    config_path = os.path.join(load_path, "config.json")
                    if os.path.exists(config_path):
                        with open(config_path, "r", encoding="utf-8") as f:
                            config = json.load(f)
                            if "aspects" in config:
                                self.aspects = config["aspects"]
                                print(f"从模型配置加载了方面列表: {self.aspects}")
                except Exception as e:
                    print(f"加载方面列表失败，将使用默认方面列表: {str(e)}")

                # 尝试加载已训练的模型（使用AutoTokenizer和AutoModelForSequenceClassification）
                print(f"正在加载已训练的模型: {load_path}")
                self.tokenizer = AutoTokenizer.from_pretrained(load_path)

                # 首次尝试 - 使用ignore_mismatched_sizes=True
                try:
                    self.model = AutoModelForSequenceClassification.from_pretrained(
                        load_path,
                        ignore_mismatched_sizes=True  # 关键参数，忽略尺寸不匹配
                    ).to(self.device)
                    print("模型加载成功(使用ignore_mismatched_sizes)！")
                except Exception as e1:
                    print(f"第一种加载方法失败: {str(e1)}")

                    # 第二次尝试 - 使用本地配置文件
                    try:
                        config = AutoConfig.from_pretrained(
                            load_path,
                            num_labels=len(self.aspects) * len(self.sentiments)
                        )
                        self.model = AutoModelForSequenceClassification.from_pretrained(
                            load_path,
                            config=config,
                            ignore_mismatched_sizes=True
                        ).to(self.device)
                        print("模型加载成功(使用自定义配置)！")
                    except Exception as e2:
                        # 如果两种方法都失败，抛出详细错误
                        raise RuntimeError(f"模型加载失败，可能词表不匹配。\n错误1: {str(e1)}\n错误2: {str(e2)}")

                print("已训练模型加载完成！")
            except Exception as e:
                # 如果失败，明确报错
                raise RuntimeError(f"无法加载已训练模型: {str(e)}")
        else:
            # 仅当明确指定不加载现有模型时才使用预训练模型
            print(f"加载预训练模型: {model_name}")
            try:
                self.tokenizer = AutoTokenizer.from_pretrained(model_name)
                self.model = AutoModelForSequenceClassification.from_pretrained(
                    model_name,
                    num_labels=len(self.aspects) * len(self.sentiments)
                ).to(self.device)
                print("预训练模型加载完成，需要进行微调训练")
            except Exception as e:
                print(f"无法加载模型 {model_name}: {str(e)}")
                print("尝试使用备用模型: bert-base-chinese")
                self.tokenizer = AutoTokenizer.from_pretrained("bert-base-chinese")
                self.model = AutoModelForSequenceClassification.from_pretrained(
                    "bert-base-chinese",
                    num_labels=len(self.aspects) * len(self.sentiments)
                ).to(self.device)

    def train(self, train_data, epochs=5, batch_size=16, learning_rate=2e-5, save_path=None, eval_data=None):
        """训练方面情感分析模型并保存"""
        print(f"开始训练方面情感模型，使用{len(train_data)}条训练数据...")

        # 检查训练数据中的方面是否都在self.aspects中
        unique_aspects = set([item["aspect"] for item in train_data])
        for aspect in unique_aspects:
            if aspect not in self.aspects:
                raise ValueError(f"训练数据中含有未定义的方面: '{aspect}'，请更新方面列表")

        # 数据预处理
        train_texts = [item["text"] for item in train_data]
        train_labels = []

        # 将(方面,情感)对转换为单一标签ID
        for item in train_data:
            aspect_idx = self.aspects.index(item["aspect"])
            sentiment_idx = self.sentiments.index(item["sentiment"])
            label = aspect_idx * len(self.sentiments) + sentiment_idx
            train_labels.append(label)

        # 准备训练数据
        encodings = self.tokenizer(train_texts, truncation=True, padding=True, max_length=128)
        train_dataset = TensorDataset(
            torch.tensor(encodings["input_ids"]),
            torch.tensor(encodings["attention_mask"]),
            torch.tensor(train_labels)
        )
        train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)

        # 准备验证数据（如果有）
        if eval_data:
            # 同样检查验证数据中的方面
            unique_eval_aspects = set([item["aspect"] for item in eval_data])
            for aspect in unique_eval_aspects:
                if aspect not in self.aspects:
                    raise ValueError(f"验证数据中含有未定义的方面: '{aspect}'，请更新方面列表")

            eval_texts = [item["text"] for item in eval_data]
            eval_labels = []
            for item in eval_data:
                aspect_idx = self.aspects.index(item["aspect"])
                sentiment_idx = self.sentiments.index(item["sentiment"])
                label = aspect_idx * len(self.sentiments) + sentiment_idx
                eval_labels.append(label)

            eval_encodings = self.tokenizer(eval_texts, truncation=True, padding=True, max_length=128)
            eval_dataset = TensorDataset(
                torch.tensor(eval_encodings["input_ids"]),
                torch.tensor(eval_encodings["attention_mask"]),
                torch.tensor(eval_labels)
            )
            eval_loader = DataLoader(eval_dataset, batch_size=batch_size)

        # 训练配置
        optimizer = AdamW(self.model.parameters(), lr=learning_rate)

        # 训练循环
        self.model.train()
        best_accuracy = 0
        for epoch in range(epochs):
            total_loss = 0
            correct = 0
            total = 0

            for batch_idx, batch in enumerate(train_loader):
                input_ids, attention_mask, labels = [b.to(self.device) for b in batch]

                # 前向传播
                outputs = self.model(
                    input_ids=input_ids,
                    attention_mask=attention_mask,
                    labels=labels
                )

                loss = outputs.loss
                total_loss += loss.item()

                # 计算准确率
                _, predicted = outputs.logits.max(1)
                correct += (predicted == labels).sum().item()
                total += labels.size(0)

                # 反向传播
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()

                # 每10个批次打印一次进度
                if (batch_idx + 1) % 10 == 0:
                    print(f"Epoch {epoch+1}/{epochs}, Batch {batch_idx+1}/{len(train_loader)}, "
                          f"Loss: {loss.item():.4f}, Acc: {correct/total:.4f}")

            # 每个epoch结束打印统计
            avg_loss = total_loss/len(train_loader) if len(train_loader) > 0 else 0
            train_accuracy = correct/total if total > 0 else 0
            print(f"Epoch {epoch+1}/{epochs} 完成, 平均损失: {avg_loss:.4f}, "
                  f"训练准确率: {train_accuracy:.4f}")

            # 如果有验证数据，进行验证
            if eval_data:
                self.model.eval()
                eval_correct = 0
                eval_total = 0
                eval_loss = 0

                with torch.no_grad():
                    for eval_batch in eval_loader:
                        input_ids, attention_mask, labels = [b.to(self.device) for b in eval_batch]
                        outputs = self.model(
                            input_ids=input_ids,
                            attention_mask=attention_mask,
                            labels=labels
                        )

                        eval_loss += outputs.loss.item()
                        _, predicted = outputs.logits.max(1)
                        eval_correct += (predicted == labels).sum().item()
                        eval_total += labels.size(0)

                eval_accuracy = eval_correct / eval_total if eval_total > 0 else 0
                avg_eval_loss = eval_loss/len(eval_loader) if len(eval_loader) > 0 else 0
                print(f"验证集损失: {avg_eval_loss:.4f}, 验证准确率: {eval_accuracy:.4f}")

                # 保存最佳模型
                if eval_accuracy > best_accuracy and save_path:
                    best_accuracy = eval_accuracy
                    self.save_model(f"{save_path}_best")
                    print(f"保存最佳模型，准确率: {best_accuracy:.4f}")

                self.model.train()

        # 训练完成，保存最终模型
        if save_path:
            self.save_model(save_path)
            print(f"模型训练完成，已保存到: {save_path}")

        return {"loss": avg_loss, "accuracy": train_accuracy}

    def save_model(self, save_path):
        """保存模型和分词器"""
        # 确保保存目录存在
        os.makedirs(save_path, exist_ok=True)

        # 保存模型和分词器
        self.model.save_pretrained(save_path)
        self.tokenizer.save_pretrained(save_path)

        # 保存方面和情感类别到配置文件
        config_path = os.path.join(save_path, "config.json")
        if os.path.exists(config_path):
            try:
                with open(config_path, "r", encoding="utf-8") as f:
                    config = json.load(f)

                # 添加方面和情感信息
                config["aspects"] = self.aspects
                config["sentiments"] = self.sentiments

                with open(config_path, "w", encoding="utf-8") as f:
                    json.dump(config, f, ensure_ascii=False, indent=2)

                print(f"已将方面和情感信息保存到配置文件")
            except Exception as e:
                print(f"保存配置信息失败: {str(e)}")
                # 创建单独的配置文件
                with open(os.path.join(save_path, "aspect_config.json"), "w", encoding="utf-8") as f:
                    json.dump({
                        "aspects": self.aspects,
                        "sentiments": self.sentiments
                    }, f, ensure_ascii=False, indent=2)

    def predict(self, text, threshold=0.1):
        """预测文本的方面和情感"""
        self.model.eval()
        results = []

        # 确保输入是单句
        # 分句逻辑应该在外部处理，predict只负责单句分析
        with torch.no_grad():
            # 预处理
            inputs = self.tokenizer(
                text,
                return_tensors="pt",
                truncation=True,
                max_length=128
            ).to(self.device)

            # 预测
            outputs = self.model(**inputs)
            logits = outputs.logits[0]
            probs = torch.softmax(logits, dim=0)

            # 获取前3个最可能的预测结果
            top_k = min(3, len(probs))
            top_probs, top_indices = torch.topk(probs, top_k)

            # 处理预测结果
            for prediction, confidence in zip(top_indices.tolist(), top_probs.tolist()):
                # 只处理置信度超过阈值的预测
                if confidence > threshold:
                    # 解码预测结果
                    aspect_idx = prediction // len(self.sentiments)
                    sentiment_idx = prediction % len(self.sentiments)

                    # 确保索引在有效范围内
                    if aspect_idx < len(self.aspects):
                        aspect = self.aspects[aspect_idx]
                        sentiment = self.sentiments[sentiment_idx]
                        results.append({
                            "aspect": aspect,
                            "sentiment": sentiment,
                            "confidence": confidence
                        })

        return results