import { check, sleep } from 'k6';
import ws from 'k6/ws';
import { Trend } from 'k6/metrics';
import { BASE_URL } from '../utils/const.js';
import { Stomp } from '../utils/stomp.js';
import { getRandomElement, getRandomDelay } from '../utils/helper.js';

// 커스텀 메트릭: 채팅 지연 시간 측정
const chatLatency = new Trend('chat_latency_ms');

export const options = {
    stages: [
        { duration: '30s', target: 100 }, // 30초동안 0명에서 300명으로 점진적증가
        { duration: '30s', target: 200 }, // 1분동안 100명에서 200명으로 점진적 증가
        { duration: '30s', target: 300 },
        { duration: '1m', target: 300 },
        { duration: '30s', target: 0 },
    ],
};

const contentId = 993;
const CF_HOST = BASE_URL.CLOUDFRONT.replace('https://', '');
const wsUrl = `wss://${CF_HOST}/ws/websocket`;

const normalReactions = ["재밌네요", "오늘 폼 미쳤다", "치킨 시킴", "심판 뭐하냐", "아쉽다"];
const goalReactions = ["와아아아아아!!!!!", "골!!!!!!!!!!!", "미쳤다!!!", "대박!!!!", "이걸 넣네!!!"];

export default function (tokens) {
    if (!tokens || tokens.length === 0) return;

    // VU마다 고유한 토큰 할당
    const myTokenInfo = tokens[(__VU - 1) % tokens.length];
    const token = myTokenInfo.accessToken;

    const params = { headers: { 'Origin': BASE_URL.CLOUDFRONT } };

    const res = ws.connect(wsUrl, params, function (socket) {
        let connectionTime = Date.now();
        let lastSendTime = 0;
        let lastSendLogTime = 0;
        let lastMessageLogTime = 0;

        socket.on('open', function () {
            // CONNECT 프레임 송신 (Raw WebSocket STOMP)
            socket.send(Stomp.connect(token));
        });

        socket.on('message', function (msg) {
            // SockJS 하트비트(h) 및 오프닝(o) 프레임 무시 (Raw면 영향 없음)
            if (msg === 'h' || msg === 'o') return;

            let stompMsg = msg;
            // SockJS 메시지 프레임(a["..."]) 처리: 껍데기를 벗겨 순수 STOMP 문자열 추출
            if (msg.startsWith('a["') && msg.endsWith('"]')) {
                try {
                    stompMsg = JSON.parse(msg.substring(1))[0];
                } catch (e) { return; }
            }

            // 1. STOMP 연결 성공 시 구독(SUBSCRIBE) 시작
            if (stompMsg.startsWith('CONNECTED')) {
                if (__VU === 1) console.log(`[1번 유저의 모든 메시지] ${msg}`); // 1번 유저의 모든 수신 메시지 출력
                socket.send(Stomp.subscribe(`sub-${__VU}`, `/sub/contents/${contentId}/chat`));

                // 채팅 발송 루프
                socket.setInterval(function () {
                    const now = Date.now();
                    const elapsedSec = (now - connectionTime) / 1000;
                    let maxSenders = 0, isGoalPhase = false;

                    // 시나리오 페이즈 제어
                    // 0~20s: 조용, 20~40s: 증가, 40~90s: 골(폭증), 90~120s: 안정
                    if (elapsedSec < 20) maxSenders = 0;
                    else if (elapsedSec < 40) maxSenders = 50;
                    else if (elapsedSec < 90) { maxSenders = 100; isGoalPhase = true; }
                    else if (elapsedSec < 120) maxSenders = 60;
                    else return;

                    if (__VU <= maxSenders) {
                        const delay = isGoalPhase ? getRandomDelay(300, 1000) : getRandomDelay(3000, 7000);
                        if (now - lastSendTime >= delay) {
                            const text = getRandomElement(isGoalPhase ? goalReactions : normalReactions);
                            // 백엔드 ContentChatSendRequest DTO 구조에 맞춘 페이로드
                            const payload = JSON.stringify({ content: `${text} | ts:${now}` });

                            // SEND 프레임 송신
                            socket.send(Stomp.send(`/pub/contents/${contentId}/chat`, payload));
                            if (now - lastSendLogTime >= 3000) {
                                console.log(`[SEND][VU ${__VU}] ${payload}`);
                                lastSendLogTime = now;
                            }
                            lastSendTime = now;
                        }
                    }
                }, 200);
            }

            // 2. 메시지 수신 및 레이턴시 측정 (다른 유저의 메시지 포함)
            // STOMP 프레임은 "\n\n" 이후가 바디(JSON)이며, 마지막에 \0이 붙음
            if (stompMsg.startsWith('MESSAGE')) {
                const splitIdx = stompMsg.indexOf('\n\n');
                if (splitIdx !== -1) {
                    const body = stompMsg.slice(splitIdx + 2).replace(/\0$/, '');
                    const now = Date.now();
                    if (now - lastMessageLogTime >= 3000) {
                        console.log(`[MESSAGE BODY][VU ${__VU}] ${body}`);
                        lastMessageLogTime = now;
                    }
                    try {
                        const data = JSON.parse(body);
                        const text = data?.content ?? '';
                        const match = text.match(/ts:(\d+)/);
                        if (match && match[1]) {
                            const sentTime = parseInt(match[1], 10);
                            chatLatency.add(Date.now() - sentTime);
                        }
                    } catch (e) {
                        // JSON 파싱 실패 시 무시
                    }
                }
            }

            if (stompMsg.includes('ERROR')) {
                console.error(`[VU ${__VU}] STOMP Error:`, stompMsg);
            }
        });

        // 시나리오 종료 시간에 맞춰 연결 닫기
        socket.setTimeout(() => socket.close(), 390000);
    });

    check(res, { 'status is 101': (r) => r && r.status === 101 });
}
