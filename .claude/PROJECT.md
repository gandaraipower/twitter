# Twitter Clone 프로젝트 구조

## 개요

이 프로젝트는 3개의 서비스로 구성된 마이크로서비스 아키텍처입니다.

```
twitter/
├── backend/          # Spring Boot (Java 21)
├── ai/               # FastAPI (Python 3.11)
├── frontend/         # Next.js 14 (TypeScript)
├── docker-compose.yml
└── .claude/
```

---

## 서비스 구성

| 서비스 | 기술 스택 | 포트 | 설명 |
|--------|-----------|------|------|
| backend | Spring Boot 3.4, JPA, JWT | 8080 | REST API, 인증, 게시글 CRUD |
| ai | FastAPI, Python | 8000 | 감정 분석, 해시태그 추천 |
| frontend | Next.js 14, TypeScript | 3000 | 웹 클라이언트 |
| mysql | MySQL 8.0 | 3306 | 데이터베이스 |

---

## Docker 명령어

### 전체 서비스 실행
```bash
docker-compose up -d --build
```

### 특정 서비스만 재빌드
```bash
docker-compose up -d --build backend
docker-compose up -d --build ai
docker-compose up -d --build frontend
```

### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f backend
docker-compose logs -f ai
docker-compose logs -f frontend
```

### 서비스 중지
```bash
# 중지 (데이터 유지)
docker-compose down

# 완전 삭제 (볼륨 포함)
docker-compose down -v
```

### 컨테이너 상태 확인
```bash
docker ps
```

---

## 로컬 개발 (Docker 없이)

각 폴더에서 직접 실행하면 핫 리로드가 지원되어 개발이 빠릅니다.

### Backend (backend/ 폴더)
```bash
cd backend
./gradlew bootRun
```
- http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

### AI (ai/ 폴더)
```bash
cd ai
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
- http://localhost:8000
- API 문서: http://localhost:8000/docs

### Frontend (frontend/ 폴더)
```bash
cd frontend
npm install
npm run dev
```
- http://localhost:3000

---

## API 엔드포인트

### Backend API

**인증**
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인 (JWT 토큰 반환)

**게시글**
- `GET /api/posts` - 목록 조회 (페이징)
- `GET /api/posts/{id}` - 상세 조회
- `POST /api/posts` - 작성 (인증 필요)
- `PUT /api/posts/{id}` - 수정 (인증 필요)
- `DELETE /api/posts/{id}` - 삭제 (인증 필요)

### AI API

- `POST /api/analyze/sentiment` - 감정 분석
- `POST /api/analyze/hashtags` - 해시태그 추천
- `POST /api/analyze` - 통합 분석
- `GET /health` - 헬스 체크

---

## 서비스 간 통신

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Frontend   │────▶│  Backend    │────▶│   MySQL     │
│  (3000)     │     │  (8080)     │     │   (3306)    │
└─────────────┘     └─────────────┘     └─────────────┘
       │
       │
       ▼
┌─────────────┐
│     AI      │
│   (8000)    │
└─────────────┘
```

- Frontend → Backend: REST API 호출 (http://localhost:8080)
- Frontend → AI: REST API 호출 (http://localhost:8000)
- Backend → MySQL: JDBC 연결

---

## 환경 변수

### Backend
| 변수 | 기본값 | 설명 |
|------|--------|------|
| SPRING_DATASOURCE_URL | jdbc:mysql://mysql:3306/twitterdb | DB 연결 URL |
| SPRING_DATASOURCE_USERNAME | dev | DB 사용자 |
| SPRING_DATASOURCE_PASSWORD | dev123 | DB 비밀번호 |

### Frontend (빌드 타임)
| 변수 | 기본값 | 설명 |
|------|--------|------|
| NEXT_PUBLIC_API_URL | http://localhost:8080 | Backend API URL |
| NEXT_PUBLIC_AI_URL | http://localhost:8000 | AI API URL |

---

## EC2 배포

```bash
# 1. EC2에서 프로젝트 클론
git clone https://github.com/gandaraipower/twitter.git
cd twitter

# 2. Docker Compose로 전체 서비스 실행
docker-compose up -d --build

# 3. 확인
docker ps
```

### 접속 URL
- Frontend: http://EC2_PUBLIC_IP:3000
- Backend API: http://EC2_PUBLIC_IP:8080
- AI API: http://EC2_PUBLIC_IP:8000

---

## 테스트 계정

| 이메일 | 비밀번호 |
|--------|----------|
| test@example.com | password123 |
