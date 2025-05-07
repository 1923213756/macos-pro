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
    """清洗文本同时保留中文和关键内容"""
    if not text:
        return ""
    
    # 先打印原始文本以便调试
    print(f"清洗前: '{text[:30]}'...")
    
    # 修改后的简化清洗逻辑 - 只移除明确的不需要字符
    # 保留所有中文、英文、数字和常用标点符号
    cleaned_text = re.sub(r'[^\u4e00-\u9fa5a-zA-Z0-9，。！？,.!?;；:：""\'\'（）()\s]', '', text)
    
    # 替换多个空格为一个空格
    cleaned_text = re.sub(r'\s+', ' ', cleaned_text).strip()
    
    # 打印清洗后的文本以便调试
    print(f"清洗后: '{cleaned_text[:30]}'...")
    
    return cleaned_text