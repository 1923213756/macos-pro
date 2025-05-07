import re

def split_into_sentences(text):
    """将文本分割成句子，使用更全面的标点符号模式"""
    pattern = r'([。！？?!;；,，\.…]+)'
    parts = re.split(pattern, text)

    sentences = []
    for i in range(0, len(parts)-1, 2):
        if parts[i].strip():
            sentences.append((parts[i] + parts[i+1]).strip())

    if len(parts) % 2 == 1 and parts[-1].strip():
        sentences.append(parts[-1].strip())

    if not sentences and text.strip():
        sentences = [text.strip()]

    return sentences

def clean_text(text):
    """清理文本，保留语义重要的内容"""
    text = re.sub(r'<[^>]+>', '', text)
    text = re.sub(r'[^\\u4e00-\\\u9fa5a-zA-Z0-9，。！？,.!?;；:：""''（）()]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text