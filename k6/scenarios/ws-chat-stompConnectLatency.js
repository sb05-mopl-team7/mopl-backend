import { check } from 'k6';
import ws from 'k6/ws';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../utils/const.js';
import { Stomp } from '../utils/stomp.js';
import { getRandomElement } from '../utils/helper.js';

const chatLatency = new Trend('chat_latency_ms');
const stompConnectLatency = new Trend('stomp_connect_latency_ms');
const ownMessageRoundTrip = new Trend('own_message_roundtrip_ms');
const sendScheduleLag = new Trend('send_schedule_lag_ms');
const sentPayloadBytes = new Trend('ws_sent_payload_bytes');
const receivedPayloadBytes = new Trend('ws_received_payload_bytes');

const chatMessagesSent = new Counter('chat_messages_sent');
const chatMessagesReceived = new Counter('chat_messages_received');
const publishTimeouts = new Counter('publish_timeout_count');

const stompErrors = new Rate('stomp_error_rate');
const messageParseFailures = new Rate('message_parse_failure_rate');
const publishTimeoutRate = new Rate('publish_timeout_rate');
const ownMessageReceiveRate = new Rate('own_message_receive_rate');

const MAX_PUBLISHERS = Number(__ENV.MAX_PUBLISHERS || 80);
const CONTENT_ID = Number(__ENV.CONTENT_ID || 993);
const PUBLISH_TIMEOUT_MS = Number(__ENV.PUBLISH_TIMEOUT_MS || 3000);
const SEND_TICK_MS = Number(__ENV.SEND_TICK_MS || 100);
const SESSION_DURATION_MS = Number(__ENV.SESSION_DURATION_MS || 240000);

const PHASES = [
    { name: 'warmup', untilMs: 30000, sendIntervalMs: 1500 },
    { name: 'load', untilMs: 90000, sendIntervalMs: 700 },
    { name: 'stress', untilMs: 150000, sendIntervalMs: 300 },
    { name: 'soak', untilMs: 210000, sendIntervalMs: 700 },
];

export const options = {
    stages: [
        { duration: '30s', target: 40 },
        { duration: '30s', target: 80 },
        { duration: '30s', target: 120 },
        { duration: '2m', target: 120 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        checks: ['rate>0.99'],
        stomp_error_rate: ['rate<0.01'],
        message_parse_failure_rate: ['rate<0.001'],
        publish_timeout_rate: ['rate<0.01'],
        own_message_receive_rate: ['rate>0.99'],
        own_message_roundtrip_ms: ['p(95)<1000', 'p(99)<2000'],
        chat_latency_ms: ['p(95)<1000', 'p(99)<2000'],
        ws_connecting: ['p(95)<1000'],
    },
};

const CF_HOST = BASE_URL.CLOUDFRONT.replace('https://', '');
const wsUrl = `wss://${CF_HOST}/ws/websocket`;

const normalReactions = ['재밌네요', '오늘 폼 미쳤다', '치킨 시킴', '심판 뭐하냐', '아쉽다'];
const stressReactions = ['와아아아아아!!!!!', '골!!!!!!!!!!!', '미쳤다!!!', '대박!!!!', '이걸 넣네!!!'];

function normalizeSetupData(data) {
    if (Array.isArray(data)) {
        return {
            tokens: data,
            scenarioStartTimeMs: Date.now(),
        };
    }

    return {
        tokens: data?.tokens ?? [],
        scenarioStartTimeMs: Number(data?.scenarioStartTimeMs || Date.now()),
    };
}

function getCurrentPhase(globalElapsedMs) {
    for (const phase of PHASES) {
        if (globalElapsedMs < phase.untilMs) {
            return phase;
        }
    }

    return null;
}

function buildMessageId(vu, seq) {
    return `${vu}:${seq}`;
}

function buildPayload(now, seq, phaseName) {
    const textPool = phaseName === 'stress' ? stressReactions : normalReactions;
    const message = getRandomElement(textPool);

    return JSON.stringify({
        content: `LOADTEST|vu=${__VU}|seq=${seq}|phase=${phaseName}|ts=${now}|msg=${message}`,
    });
}

function parseMessageMarker(content) {
    const match = content.match(/^LOADTEST\|vu=(\d+)\|seq=(\d+)\|phase=([a-z]+)\|ts=(\d+)\|msg=/);
    if (!match) return null;

    return {
        vu: Number(match[1]),
        seq: Number(match[2]),
        phase: match[3],
        sentAt: Number(match[4]),
    };
}

export default function (data) {
    const { tokens, scenarioStartTimeMs } = normalizeSetupData(data);
    if (!tokens || tokens.length === 0) return;

    const myTokenInfo = tokens[(__VU - 1) % tokens.length];
    const token = myTokenInfo.accessToken;
    const isPublisher = __VU <= MAX_PUBLISHERS;

    const params = { headers: { Origin: BASE_URL.CLOUDFRONT } };

    const res = ws.connect(wsUrl, params, function (socket) {
        const connectionTime = Date.now();
        const pendingMessages = new Map();
        let lastSendTime = 0;
        let lastPhaseName = 'idle';
        let sequence = 0;
        let socketClosed = false;

        socket.on('open', function () {
            socket.send(Stomp.connect(token));
        });

        socket.on('message', function (msg) {
            if (msg === 'h' || msg === 'o') return;

            let stompMsg = msg;
            if (msg.startsWith('a["') && msg.endsWith('"]')) {
                try {
                    stompMsg = JSON.parse(msg.substring(1))[0];
                } catch (e) {
                    messageParseFailures.add(true);
                    return;
                }
            }

            if (stompMsg.startsWith('CONNECTED')) {
                stompConnectLatency.add(Date.now() - connectionTime);
                socket.send(Stomp.subscribe(`sub-${__VU}`, `/sub/contents/${CONTENT_ID}/chat`));

                socket.setInterval(function () {
                    const now = Date.now();
                    const globalElapsedMs = now - scenarioStartTimeMs;
                    const currentPhase = getCurrentPhase(globalElapsedMs);

                    if (!currentPhase) return;

                    lastPhaseName = currentPhase.name;

                    if (!isPublisher) return;

                    if (lastSendTime > 0) {
                        const scheduleLag = Math.max(0, now - lastSendTime - currentPhase.sendIntervalMs);
                        sendScheduleLag.add(scheduleLag);
                    }

                    if (lastSendTime === 0 || now - lastSendTime >= currentPhase.sendIntervalMs) {
                        sequence += 1;
                        const messageId = buildMessageId(__VU, sequence);
                        const payload = buildPayload(now, sequence, currentPhase.name);

                        pendingMessages.set(messageId, now);
                        socket.send(Stomp.send(`/pub/contents/${CONTENT_ID}/chat`, payload));

                        chatMessagesSent.add(1);
                        sentPayloadBytes.add(payload.length);
                        lastSendTime = now;
                    }
                }, SEND_TICK_MS);

                socket.setInterval(function () {
                    const now = Date.now();
                    for (const [messageId, sentAt] of pendingMessages.entries()) {
                        if (now - sentAt < PUBLISH_TIMEOUT_MS) continue;

                        pendingMessages.delete(messageId);
                        publishTimeouts.add(1);
                        publishTimeoutRate.add(true);
                        ownMessageReceiveRate.add(false);
                    }
                }, 500);

                socket.setTimeout(function () {
                    if (socketClosed) return;
                    socketClosed = true;
                    socket.close();
                }, SESSION_DURATION_MS);

                return;
            }

            if (stompMsg.startsWith('MESSAGE')) {
                const splitIdx = stompMsg.indexOf('\n\n');
                if (splitIdx === -1) return;

                const body = stompMsg.slice(splitIdx + 2).replace(/\0$/, '');
                const now = Date.now();

                chatMessagesReceived.add(1);
                receivedPayloadBytes.add(body.length);

                try {
                    const data = JSON.parse(body);
                    const content = data?.content ?? '';
                    const marker = parseMessageMarker(content);

                    if (marker) {
                        chatLatency.add(now - marker.sentAt);

                        if (marker.vu === __VU) {
                            const messageId = buildMessageId(marker.vu, marker.seq);
                            const sentAt = pendingMessages.get(messageId);

                            if (sentAt) {
                                pendingMessages.delete(messageId);
                                ownMessageRoundTrip.add(now - sentAt);
                                ownMessageReceiveRate.add(true);
                                publishTimeoutRate.add(false);
                            }
                        }
                    }

                    messageParseFailures.add(false);
                } catch (e) {
                    messageParseFailures.add(true);
                }

                return;
            }

            if (stompMsg.includes('ERROR')) {
                stompErrors.add(true);
                console.error(`[VU ${__VU}][${lastPhaseName}] STOMP Error: ${stompMsg}`);
                return;
            }

            stompErrors.add(false);
        });
    });

    check(res, { 'status is 101': (r) => r && r.status === 101 });
}