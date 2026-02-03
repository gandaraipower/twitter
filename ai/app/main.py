from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import re

app = FastAPI(
    title="Twitter AI Service",
    description="AI Service for Twitter Clone",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request/Response Models
class AnalyzeRequest(BaseModel):
    content: str


class SentimentResponse(BaseModel):
    sentiment: str  # positive, negative, neutral
    score: float
    emoji: str


class HashtagResponse(BaseModel):
    hashtags: list[str]


# 감정 분석용 키워드 (데모용 - 실제로는 ML 모델 사용)
POSITIVE_WORDS = [
    "좋아", "최고", "행복", "감사", "사랑", "기쁨", "즐거", "멋지", "훌륭", "대박",
    "good", "great", "happy", "love", "thanks", "awesome", "amazing", "best", "nice", "wonderful"
]

NEGATIVE_WORDS = [
    "싫어", "최악", "슬프", "화나", "짜증", "힘들", "어렵", "나쁘", "별로", "실망",
    "bad", "hate", "sad", "angry", "terrible", "worst", "difficult", "disappointed", "awful", "horrible"
]


def analyze_sentiment(text: str) -> tuple[str, float, str]:
    """간단한 키워드 기반 감정 분석"""
    text_lower = text.lower()

    positive_count = sum(1 for word in POSITIVE_WORDS if word in text_lower)
    negative_count = sum(1 for word in NEGATIVE_WORDS if word in text_lower)

    total = positive_count + negative_count
    if total == 0:
        return "neutral", 0.5, "😐"

    positive_ratio = positive_count / total

    if positive_ratio > 0.6:
        score = 0.5 + (positive_ratio * 0.5)
        return "positive", round(score, 2), "😊"
    elif positive_ratio < 0.4:
        score = 0.5 - ((1 - positive_ratio) * 0.5)
        return "negative", round(score, 2), "😢"
    else:
        return "neutral", 0.5, "😐"


def extract_hashtags(text: str) -> list[str]:
    """텍스트에서 해시태그 추천"""
    # 기존 해시태그 추출
    existing = re.findall(r'#(\w+)', text)

    # 키워드 기반 추천 (데모용)
    keywords = {
        "날씨": ["날씨", "weather", "비", "눈", "맑음", "흐림"],
        "음식": ["맛있", "먹", "밥", "식사", "커피", "food", "eat", "delicious"],
        "여행": ["여행", "travel", "trip", "휴가", "vacation"],
        "일상": ["오늘", "today", "daily", "하루"],
        "운동": ["운동", "헬스", "gym", "fitness", "workout"],
        "개발": ["코딩", "개발", "프로그래밍", "coding", "dev", "programming"],
        "공부": ["공부", "study", "학습", "learning"],
    }

    recommended = set(existing)
    text_lower = text.lower()

    for tag, words in keywords.items():
        if any(word in text_lower for word in words):
            recommended.add(tag)

    # 최대 5개 반환
    return list(recommended)[:5]


@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "ai"}


@app.get("/")
def root():
    return {"message": "Twitter AI Service"}


@app.post("/api/analyze/sentiment", response_model=SentimentResponse)
def analyze_sentiment_api(request: AnalyzeRequest):
    """게시글 감정 분석 API"""
    sentiment, score, emoji = analyze_sentiment(request.content)
    return SentimentResponse(sentiment=sentiment, score=score, emoji=emoji)


@app.post("/api/analyze/hashtags", response_model=HashtagResponse)
def recommend_hashtags_api(request: AnalyzeRequest):
    """해시태그 추천 API"""
    hashtags = extract_hashtags(request.content)
    return HashtagResponse(hashtags=hashtags)


@app.post("/api/analyze")
def analyze_all(request: AnalyzeRequest):
    """감정 분석 + 해시태그 추천 통합 API"""
    sentiment, score, emoji = analyze_sentiment(request.content)
    hashtags = extract_hashtags(request.content)

    return {
        "sentiment": {
            "sentiment": sentiment,
            "score": score,
            "emoji": emoji
        },
        "hashtags": hashtags
    }
