## K6 디렉토리 구조
```aiignore
k6/
├── scenarios/
│   └── playlist_create.js          // 실제 테스트 시나리오
│
├── utils/
│   ├── const.js            // urls
│   ├── stomp.js            // STOMP 프레임 생성
│   ├── metrics.js          // custom metrics
│   ├── helpers.js          // 랜덤, 파싱 등
│
├── setup/
│   └── login.js            // 토큰 발급 (setup)
│
├── data/
│   ├── mopl_users.csv      // User 테이블 CSV파일
│   └── users.js            // CSV 로딩
│
├── .env.k6
└── main.js                 // entry point (k6 run 대상)
```

## 테스트 실행 방법
```aiignore
cp .env.k6.example .env.k6
docker compose -f docker-compose-k6.yml up k6
```

## 환경변수
`k6`는 `.env` 파일을 자동으로 읽지 않아서, Docker Compose가 `.env.k6`를 읽어 컨테이너 환경변수로 주입하도록 설정했습니다.

`.env.k6` 예시:
```aiignore
K6_BASE_URL_ALB=https://your-api.example.com
K6_BASE_URL_CLOUDFRONT=https://your-cloudfront.example.com
K6_BASE_URL_LOCAL=http://host.docker.internal:8080
```
