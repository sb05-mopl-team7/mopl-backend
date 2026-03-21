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
└── main.js                 // entry point (k6 run 대상)
```

## 테스트 실행 방법
```aiignore
docker compose -f docker-compose-k6.yml up k6
```