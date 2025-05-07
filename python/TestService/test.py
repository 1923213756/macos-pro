import requests
import json

url = "http://localhost:5001/analyze_batch"
payload = {
    "reviews": [
        "感觉服务态度不错，但是量比较小，味道还行",
        "环境很好，服务员态度也很好，就是价格有点贵",
        "菜品味道一般，不值这个价，不推荐",
        "位置很好找，停车方便，厨师手艺很棒！",
        "分量足，价格实惠，非常满意，下次还会再来"
    ]
}

headers = {'Content-Type': 'application/json'}
response = requests.post(url, data=json.dumps(payload), headers=headers)
print(response.status_code)
print(json.dumps(response.json(), ensure_ascii=False, indent=2))