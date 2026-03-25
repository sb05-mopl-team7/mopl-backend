import { check } from 'k6';
import ws from 'k6/ws';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../utils/const.js';
import { Stomp } from '../utils/stomp.js';
import { getRandomElement } from '../utils/helper.js';

const wsConnectLatency = new Trend('ws_connect_latency_ms');
const stompConnectLatency = new Trend('stomp_connect_latency_ms');
const subscribeReadyLatency = new Trend('subscribe_ready_latency_ms');
const broadcastDeliveryLatency = new Trend('broadcast_delivery_latency_ms');
const ownMessageRoundTrip = new Trend('own_message_roundtrip_ms');
const sendScheduleDrift = new Trend('send_schedule_drift_ms');
const sentPayloadBytes = new Trend('ws_sent_payload_bytes');
const receivedPayloadBytes = new Trend('ws_received_payload_bytes');

const broadcastMessagesSent = new Counter('broadcast_messages_sent');
const broadcastMessagesReceived = new Counter('broadcast_messages_received');
const ownMessageTimeouts = new Counter('own_message_timeout_count');
const stompErrorCount = new Counter('stomp_error_count');
const parseFailureCount = new Counter('message_parse_failure_count');

const wsUpgradeSuccessRate = new Rate('ws_upgrade_success_rate');
const stompConnectSuccessRate = new Rate('stomp_connect_success_rate');
const ownMessageReceiveRate = new Rate('own_message_receive_rate');
const messageParseFailureRate = new Rate('message_parse_failure_rate');

const CONTENT_ID = Number(__ENV.CONTENT_ID || 993);
const MAX_PUBLISHERS = Number(__ENV.MAX_PUBLISHERS || 120);
const SESSION_DURATION_MS = Number(__ENV.SESSION_DURATION_MS || 240000);
const SEND_TICK_MS = Number(__ENV.SEND_TICK_MS || 100);
const PUBLISH_TIMEOUT_MS = Number(__ENV.PUBLISH_TIMEOUT_MS || 5000);
const SUBSCRIPTION_SETTLE_MS = Number(__ENV.SUBSCRIPTION_SETTLE_MS || 1000);

const PHASES = [
    { name: 'join', untilMs: 15000, sendIntervalMs: null },
    { name: 'warmup', untilMs: 45000, sendIntervalMs: 2200 },
    { name: 'steady', untilMs: 120000, sendIntervalMs: 1200 },
    { name: 'burst', untilMs: 180000, sendIntervalMs: 450 },
    { name: 'cooldown', untilMs: 225000, sendIntervalMs: 1800 },
];

export const options = {
    stages: [
        { duration: '30s', target: 40 },
        { duration: '30s', target: 80 },
        { duration: '30s', target: 120 },
        { duration: '1m', target: 120 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        checks: ['rate>0.99'],
        ws_upgrade_success_rate: ['rate>0.99'],
        stomp_connect_success_rate: ['rate>0.99'],
        message_parse_failure_rate: ['rate<0.001'],
        own_message_receive_rate: ['rate>0.99'],
        ws_connect_latency_ms: ['p(95)<1500'],
        stomp_connect_latency_ms: ['p(95)<1500'],
        subscribe_ready_latency_ms: ['p(95)<2500'],
        own_message_roundtrip_ms: ['p(95)<1500', 'p(99)<3000'],
        broadcast_delivery_latency_ms: ['p(95)<1500', 'p(99)<3000'],
    },
};

const CF_HOST = BASE_URL.CLOUDFRONT.replace('https://', '');
const wsUrl = `wss://${CF_HOST}/ws/websocket`;

const normalMessages = ['재밌네요', '수비 좋다', '집중력 좋네', '흐름 괜찮다', '템포 빠르다'];
const burstMessages = ['와 미쳤다', '이걸 넣네', '대박이다', '분위기 탄다', '오늘 폼 장난 아니다'];

function normalizeSetupData(data) {
    if (Array.isArray(data)) {
        return { tokens: data };
    }

    return { tokens: data?.tokens ?? [] };
}

function unwrapSockJsFrame(frame) {
    if (frame === 'h' || frame === 'o') return null;
    if (!frame.startsWith('a["') || !frame.endsWith('"]')) return frame;

    const messages = JSON.parse(frame.substring(1));
    return messages[0] || null;
}

function getCurrentPhase(sessionElapsedMs) {
    for (const phase of PHASES) {
        if (sessionElapsedMs < phase.untilMs) {
            return phase;
        }
    }

    return null;
}

function buildMessageId(vu, seq) {
    return `${vu}:${seq}`;
}

function buildPayload(now, sequence, phaseName) {
    const textPool = phaseName === 'burst' ? burstMessages : normalMessages;
    const text = getRandomElement(textPool);

    return JSON.stringify({
        content: `K6CHAT|vu=${__VU}|seq=${sequence}|phase=${phaseName}|ts=${now}|msg=${text}`,
    });
}

function parseMarker(content) {
    const match = content.match(/^K6CHAT\|vu=(\d+)\|seq=(\d+)\|phase=([a-z]+)\|ts=(\d+)\|msg=/);
    if (!match) return null;

    return {
        senderVu: Number(match[1]),
        sequence: Number(match[2]),
        phase: match[3],
        sentAt: Number(match[4]),
    };
}

function addParseFailure() {
    parseFailureCount.add(1);
    messageParseFailureRate.add(true);
}

export default function (data) {
    const { tokens } = normalizeSetupData(data);
    if (!tokens.length) return;

    const myTokenInfo = tokens[(__VU - 1) % tokens.length];
    const token = myTokenInfo.accessToken;
    const isPublisher = __VU <= MAX_PUBLISHERS;
    const params = { headers: { Origin: BASE_URL.CLOUDFRONT } };

    const res = ws.connect(wsUrl, params, function (socket) {
        const connectionStartedAt = Date.now();
        const pendingOwnMessages = new Map();
        let connectedAt = 0;
        let subscribeReadyAt = 0;
        let sequence = 0;
        let lastSendAt = 0;
        let socketClosed = false;

        socket.on('open', function () {
            wsConnectLatency.add(Date.now() - connectionStartedAt);
            socket.send(Stomp.connect(token));
        });

        socket.on('message', function (frame) {
            let stompFrame;

            try {
                stompFrame = unwrapSockJsFrame(frame);
            } catch (e) {
                addParseFailure();
                return;
            }

            if (!stompFrame) return;

            if (stompFrame.startsWith('CONNECTED')) {
                connectedAt = Date.now();
                stompConnectLatency.add(connectedAt - connectionStartedAt);
                stompConnectSuccessRate.add(true);

                socket.send(Stomp.subscribe(`sub-${__VU}`, `/sub/contents/${CONTENT_ID}/chat`));

                socket.setTimeout(function () {
                    subscribeReadyAt = Date.now();
                    subscribeReadyLatency.add(subscribeReadyAt - connectionStartedAt);
                }, SUBSCRIPTION_SETTLE_MS);

                socket.setInterval(function () {
                    if (!isPublisher || subscribeReadyAt === 0) return;

                    const now = Date.now();
                    const phase = getCurrentPhase(now - connectedAt);
                    if (!phase || phase.sendIntervalMs === null) return;

                    if (lastSendAt > 0) {
                        const drift = Math.max(0, now - lastSendAt - phase.sendIntervalMs);
                        sendScheduleDrift.add(drift);
                    }

                    if (lastSendAt !== 0 && now - lastSendAt < phase.sendIntervalMs) return;

                    sequence += 1;
                    const payload = buildPayload(now, sequence, phase.name);
                    const messageId = buildMessageId(__VU, sequence);

                    pendingOwnMessages.set(messageId, now);
                    socket.send(Stomp.send(`/pub/contents/${CONTENT_ID}/chat`, payload));

                    broadcastMessagesSent.add(1);
                    sentPayloadBytes.add(payload.length);
                    lastSendAt = now;
                }, SEND_TICK_MS);

                socket.setInterval(function () {
                    const now = Date.now();

                    for (const [messageId, sentAt] of pendingOwnMessages.entries()) {
                        if (now - sentAt < PUBLISH_TIMEOUT_MS) continue;

                        pendingOwnMessages.delete(messageId);
                        ownMessageTimeouts.add(1);
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

            if (stompFrame.startsWith('MESSAGE')) {
                if (subscribeReadyAt === 0) return;

                const splitIdx = stompFrame.indexOf('\n\n');
                if (splitIdx === -1) {
                    addParseFailure();
                    return;
                }

                const body = stompFrame.slice(splitIdx + 2).replace(/\0$/, '');
                const receivedAt = Date.now();

                broadcastMessagesReceived.add(1);
                receivedPayloadBytes.add(body.length);

                try {
                    const parsed = JSON.parse(body);
                    const content = parsed?.content ?? '';
                    const marker = parseMarker(content);

                    if (!marker) {
                        messageParseFailureRate.add(false);
                        return;
                    }

                    broadcastDeliveryLatency.add(receivedAt - marker.sentAt);
                    messageParseFailureRate.add(false);

                    if (marker.senderVu !== __VU) return;

                    const messageId = buildMessageId(marker.senderVu, marker.sequence);
                    const sentAt = pendingOwnMessages.get(messageId);

                    if (!sentAt) return;

                    pendingOwnMessages.delete(messageId);
                    ownMessageRoundTrip.add(receivedAt - sentAt);
                    ownMessageReceiveRate.add(true);
                    return;
                } catch (e) {
                    addParseFailure();
                    return;
                }
            }

            if (stompFrame.includes('ERROR')) {
                stompErrorCount.add(1);
            }
        });
    });

    const upgraded = check(res, { 'status is 101': (r) => r && r.status === 101 });
    wsUpgradeSuccessRate.add(upgraded);
}
