import http from 'k6/http';
import ws from 'k6/ws';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { sleep } from 'k6';

const chatLatency = new Trend('chat_latency_ms');
const ALB_BASE_URL = 'mopl-alb-644365432.ap-northeast-2.elb.amazonaws.com';
const WS_URL = `wss://d1ocfp6g80vipy.cloudfront.net/ws/websocket`;
const CONTENT_ID = 993;

// 1. 유저 데이터 로드 csv (전역 공유)
const users = new SharedArray('users', function () {
    const file = open('./mopl_users.csv');
    const lines = file.split('\n');
    return lines.slice(1)
        .filter(line => line.trim() !== '')
        .map(line => line.split(',')[2].replace(/"/g, ''));
});

export const options = {
    stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 500 },
        { duration: '3m', target: 1000 },
        { duration: '1m', target: 0 },
    ],
};

// 테스트 시작 전 로그인으로 토큰 추출
export function setup() {
    const tokens = [];
    const batchSize = 100; // 한 번에 100명씩 병렬 로그인
    const loginCount = 200

    console.log(`[Setup] Starting parallel login for ${loginCount} users...`);

    for (let i = 0; i < loginCount; i += batchSize) {
        const batchRequests = [];

        // 1. 현재 배치(100명)에 대한 요청 객체 생성
        for (let j = i; j < i + batchSize && j < loginCount; j++) {
            batchRequests.push({
                method: 'POST',
                url: `http://${ALB_BASE_URL}/api/auth/sign-in`,
                body: {
                    username: users[j],
                    password: 'mopl1234',
                },
                params: { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } },
            });
        }

        // 2. http.batch로 병렬 요청 실행
        const responses = http.batch(batchRequests);

        // 3. 결과 수집
        responses.forEach((res, index) => {
            if (res.status === 200 && res.body) {
                try {
                    const body = res.json();
                    if (body.accessToken) tokens.push(body.accessToken);
                } catch (e) { /* 에러 처리 */ }
            }
        });

        console.log(`Progress: ${tokens.length} tokens collected...`);
        sleep(1); // 서버 부하 조절을 위해 배치 사이의 짧은 휴식
    }

    if (tokens.length === 0) throw new Error("로그인 실패");
    return tokens;
}

// 3. 순수 웹소켓 부하 테스트 (HTTP 호출 없음)
export default function (tokens) {
    // 자신의 VU 번호에 맞는 토큰 할당
    const myToken = tokens[(__VU - 1) % tokens.length];
    if (!myToken) return;

    const params = { headers: { 'Origin': `http://${ALB_BASE_URL}` } };
    const reactions = ["와 골이다!!!", "미쳤다...", "대~한민국!!"];

    const res = ws.connect(WS_URL, params, function (socket) {

        let intervalId;

        socket.on('open', function () {
            // 이미 가지고 있는 토큰으로 즉시 연결
            socket.send(`CONNECT\naccept-version:1.1,1.2\nAuthorization:Bearer ${myToken}\n\n\0`);
        });

        socket.on('message', function (msg) {
            if (msg.startsWith('CONNECTED')) {
                socket.send(`SUBSCRIBE\nid:sub-${__VU}\ndestination:/sub/contents/${CONTENT_ID}/chat\n\n\0`);

                // 채팅 발송 루프
                intervalId = socket.setInterval(function () {
                    const payload = JSON.stringify({
                        content: `${reactions[Math.floor(Math.random() * reactions.length)]} | ts:${Date.now()}`
                    });
                    socket.send(`SEND\ndestination:/pub/contents/${CONTENT_ID}/chat\ncontent-type:application/json\nAuthorization:Bearer ${myToken}\n\n${payload}\0`);
                }, Math.floor(Math.random() * 5000) + 3000);
            }

            // 지연 시간 측정
            if (msg.includes('MESSAGE') && msg.includes('ts:')) {
                const match = msg.match(/ts:(\d+)/);
                if (match) chatLatency.add(Date.now() - parseInt(match[1], 10));
            }
        });

        // [수정] 300초(5분)가 아니라, 적절한 생존 시간을 주거나
        // k6의 테스트 종료 신호를 감지해야 합니다.
        // 간단한 해결책: 실행 시간을 고정하지 않고 VU가 종료될 때 닫히게 설정
        socket.setTimeout(() => {
            socket.clearInterval(intervalId);
            socket.close();
        }, 60000); // 테스트 상황에 맞춰 조절 (예: 60초 후 종료)
    });

    check(res, { 'websocket connected': (r) => r && r.status === 101 });
}