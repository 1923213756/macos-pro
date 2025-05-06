import os
import torch
from transformers import BartTokenizer, BartForConditionalGeneration, Seq2SeqTrainingArguments, Seq2SeqTrainer
from torch.utils.data import Dataset

class SummarizationDataset(Dataset):
    def __init__(self, texts, summaries, tokenizer, max_input_length=512, max_output_length=128):
        self.tokenizer = tokenizer
        self.texts = texts
        self.summaries = summaries
        self.max_input_length = max_input_length
        self.max_output_length = max_output_length

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        text = self.texts[idx]
        summary = self.summaries[idx]

        # 处理输入
        input_encodings = self.tokenizer(
            text,
            max_length=self.max_input_length,
            truncation=True,
            padding="max_length",
            return_tensors="pt"
        )

        # 处理输出
        with self.tokenizer.as_target_tokenizer():
            output_encodings = self.tokenizer(
                summary,
                max_length=self.max_output_length,
                truncation=True,
                padding="max_length",
                return_tensors="pt"
            )

        # 从batch维度中移除
        input_ids = input_encodings['input_ids'].squeeze()
        attention_mask = input_encodings['attention_mask'].squeeze()
        labels = output_encodings['input_ids'].squeeze()

        # 将填充标记替换为-100，以便在计算损失时忽略这些标记
        labels[labels == self.tokenizer.pad_token_id] = -100

        return {
            'input_ids': input_ids,
            'attention_mask': attention_mask,
            'labels': labels
        }

class SummaryModel:
    def __init__(self, model_name="fnlp/bart-base-chinese", load_path=None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else
                                   "mps" if torch.backends.mps.is_available() else "cpu")
        print(f"使用设备: {self.device}")

        # 简化模型加载逻辑
        if load_path is None:
            # 如果没有指定加载路径，直接加载预训练模型
            print(f"加载预训练模型: {model_name}")
            self.tokenizer = BartTokenizer.from_pretrained(model_name)
            self.model = BartForConditionalGeneration.from_pretrained(model_name).to(self.device)
            print("预训练模型加载完成，需要进行微调训练")
        else:
            # 仅当明确指定加载路径时，才尝试加载
            try:
                print(f"正在加载已训练的模型: {load_path}")
                self.tokenizer = BartTokenizer.from_pretrained(load_path)
                self.model = BartForConditionalGeneration.from_pretrained(load_path).to(self.device)
                print("已训练模型加载完成！")
            except Exception as e:
                print(f"加载已训练模型失败: {str(e)}")
                print(f"将加载预训练模型: {model_name}")
                self.tokenizer = BartTokenizer.from_pretrained(model_name)
                self.model = BartForConditionalGeneration.from_pretrained(model_name).to(self.device)
                print("预训练模型加载完成，需要进行微调训练")

    def train(self, train_texts, train_summaries,
              eval_texts=None, eval_summaries=None,
              epochs=3, batch_size=4, learning_rate=5e-5,
              save_path=None):
        """训练摘要生成模型"""
        print(f"开始训练摘要模型，使用{len(train_texts)}对训练数据...")

        # 创建训练数据集
        train_dataset = SummarizationDataset(
            texts=train_texts,
            summaries=train_summaries,
            tokenizer=self.tokenizer
        )

        # 创建验证数据集（如果有）
        eval_dataset = None
        if eval_texts and eval_summaries:
            eval_dataset = SummarizationDataset(
                texts=eval_texts,
                summaries=eval_summaries,
                tokenizer=self.tokenizer
            )

        # 设置训练参数
        training_args = Seq2SeqTrainingArguments(
            output_dir="./results",
            num_train_epochs=epochs,
            per_device_train_batch_size=batch_size,
            per_device_eval_batch_size=batch_size,
            warmup_steps=500,
            weight_decay=0.01,
            logging_dir="./logs",
            logging_steps=100,
            save_steps=500,
            eval_steps=500,
            evaluation_strategy="epoch" if eval_dataset else "no",
            save_total_limit=2,
            predict_with_generate=True,
            fp16=True if torch.cuda.is_available() else False,
            learning_rate=learning_rate
        )

        # 创建训练器
        trainer = Seq2SeqTrainer(
            model=self.model,
            args=training_args,
            train_dataset=train_dataset,
            eval_dataset=eval_dataset,
        )

        # 开始训练
        print("开始训练...")
        trainer.train()

        # 保存模型
        if save_path:
            self.save_model(save_path)
            print(f"模型训练完成，已保存到: {save_path}")

        # 如果有验证数据集，进行评估
        if eval_dataset:
            results = trainer.evaluate()
            print(f"验证集评估结果: {results}")

        return trainer

    def save_model(self, save_path):
        """保存模型和分词器"""
        # 确保保存目录存在
        os.makedirs(save_path, exist_ok=True)

        # 保存模型和分词器
        self.model.save_pretrained(save_path)
        self.tokenizer.save_pretrained(save_path)

    def generate_summary(self, texts, max_length=128, min_length=30, num_beams=4):
        """生成摘要"""
        self.model.eval()

        # 将多条评论合并为单个文本
        if isinstance(texts, list):
            text = " ".join(texts)
        else:
            text = texts

        # 预处理
        inputs = self.tokenizer(text,
                                return_tensors="pt",
                                max_length=512,
                                truncation=True).to(self.device)

        # 生成摘要
        summary_ids = self.model.generate(
            inputs["input_ids"],
            max_length=max_length,
            min_length=min_length,
            num_beams=num_beams,
            early_stopping=True,
            no_repeat_ngram_size=2
        )

        # 解码
        summary = self.tokenizer.decode(summary_ids[0], skip_special_tokens=True)

        return summary