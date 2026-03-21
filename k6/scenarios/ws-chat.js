import { check, sleep } from 'k6';
import ws from 'k6/ws';
import { Trend } from 'k6/metrics';
import { BASE_URL } from '../utils/const.js';
import { Stomp } from '../utils/stomp.js';
import { getRandomElement, getRandomDelay } from '../utils/helper.js'; // 경로 수정: ../

// 1. 커스텀 메트릭 정의
const chatLatency = new Trend('chat_latency_ms');

// 3. 부하 테스트 옵션
export const options = {
    stages: [
        { duration: '2m', target: 500 },  // 2분 동안 500명 점진적 접속
        { duration: '2m', target: 500 },  // Phase 1: 평시 채팅 (50명)
        { duration: '1m', target: 500 },  // Phase 2: 골 터짐 (100명 폭주)
        { duration: '1m', target: 500 },  // Phase 3: 진정기 (60명)
        { duration: '30s', target: 0 },   // 종료
    ],
};

const contentId = 993;
// CloudFront URL에서 호스트만 추출 (wss 연결용)
const CF_HOST = BASE_URL.CLOUDFRONT.replace('https://', '');
const wsUrl = `wss://${CF_HOST}/ws/websocket`;

const normalReactions = ["재밌네요", "오늘 폼 미쳤다", "치킨 시킴", "심판 뭐하냐", "아쉽다"];
const goalReactions = ["와아아아아아!!!!!", "골!!!!!!!!!!!", "미쳤다!!!", "대박!!!!", "이걸 넣네!!!"];

export default function (tokens) {
    // setup에서 반환된 토큰 배열에서 각 VU에 맞는 토큰 할당
    const myTokenInfo = tokens[(__VU - 1) % tokens.length];
    const token = myTokenInfo.accessToken;

    const params = {
        headers: { 'Origin': BASE_URL.CLOUDFRONT }
    };

    const res = ws.connect(wsUrl, params, function (socket) {
        let connectionTime = Date.now();
        let lastSendTime = 0;

        socket.on('open', function () {
            // stomp.js를 활용한 연결 (heart-beat 추가)
            socket.send(Stomp.connect(token));
        });

        socket.on('message', function (msg) {
            // 1. 연결 성공 시 구독 시작
            if (msg.startsWith('CONNECTED')) {
                socket.send(Stomp.subscribe(`sub-${__VU}`, `/sub/contents/${contentId}/chat`));

                // 채팅 발송 루프
                socket.setInterval(function () {
                    const now = Date.now();
                    const elapsedSec = (now - connectionTime) / 1000;

                    let maxSenders = 0;
                    let isGoalPhase = false;

                    // 시나리오 페이즈 제어
                    if (elapsedSec < 120) maxSenders = 0;
                    else if (elapsedSec < 240) maxSenders = 50;
                    else if (elapsedSec < 300) { maxSenders = 100; isGoalPhase = true; }
                    else if (elapsedSec < 360) maxSenders = 60;
                    else return;

                    // 선정된 인원만 채팅 발송
                    if (__VU <= maxSenders) {
                        // 페이즈별 랜덤 딜레이 적용
                        const delay = isGoalPhase ? getRandomDelay(300, 1000) : getRandomDelay(3000, 7000);

                        if (now - lastSendTime >= delay) {
                            const text = getRandomElement(isGoalPhase ? goalReactions : normalReactions);
                            const payload = JSON.stringify({
                                content: `${text} | ts:${now}`
                            });

                            socket.send(Stomp.send(`/pub/contents/${contentId}/chat`, token, payload));
                            lastSendTime = now;
                        }
                    }
                }, 200); // 0.2초마다 상태 체크
            }

            // 2. 메시지 수신 시 레이턴시 측정
            if (msg.includes('MESSAGE') && msg.includes('ts:')) {
                const match = msg.match(/ts:(\d+)/);
                if (match) {
                    const sentTime = parseInt(match[1], 10);
                    chatLatency.add(Date.now() - sentTime);
                }
            }

            if (msg.includes('ERROR')) {
                console.error(`[VU ${__VU}] STOMP Error:`, msg);
            }
        });

        // 6분 30초 후 연결 종료
        socket.setTimeout(() => socket.close(), 390000);
    });

    check(res, { 'status is 101': (r) => r && r.status === 101 });
}