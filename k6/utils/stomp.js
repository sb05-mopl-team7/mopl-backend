export const Stomp = {
    // 연결: 인터셉터가 여기서 'Authorization' 헤더를 추출합니다.
    connect: (token) => {
        return `CONNECT\naccept-version:1.2\nAuthorization:Bearer ${token}\nheart-beat:10000,10000\n\n\0`;
    },
    // 구독: 한번 연결되면 세션이 유지되므로 토큰이 필요 없습니다.
    subscribe: (id, destination) => {
        return `SUBSCRIBE\nid:${id}\ndestination:${destination}\nack:auto\n\n\0`;
    },
    // 메시지 발송: destination과 payload 사이의 빈 줄이 중요합니다.
    send: (destination, payload) => {
        return `SEND\ndestination:${destination}\ncontent-type:application/json\n\n${payload}\n\0`;
    }
};