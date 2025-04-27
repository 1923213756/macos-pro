from fastapi import FastAPI
from transformers import pipeline

app = FastAPI()

# 加载轻量化模型
summarizer = pipeline("summarization", model="t5-small")
sentiment_analyzer = pipeline("sentiment-analysis")

@app.post("/summarize")
async def summarize(comments: list[str]):
    combined_text = " ".join(comments)
    summary = summarizer(combined_text, max_length=100, min_length=20, do_sample=False)
    return {word: summary[0]["summary_text"].count(word) for word in combined_text.split()}

@app.post("/sentiment")
async def sentiment(comment: str):
    result = sentiment_analyzer(comment)
    return result[0]["label"]