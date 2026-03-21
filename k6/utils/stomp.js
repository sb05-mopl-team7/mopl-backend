export const Stomp = {
    // 연결 프레임 생성
    connect: (token) => {
        return `CONNECT\naccept-version:1.1,1.2\nAuthorization:Bearer ${token}\n\n\0`;
    },
    // 구독 프레임 생성
    subscribe: (id, destination) => {
        return `SUBSCRIBE\nid:${id}\ndestination:${destination}\n\n\0`;
    },
    // 메시지 발송 프레임 생성
    send: (destination, token, payload) => {
        return `SEND\ndestination:${destination}\ncontent-type:application/json\nAuthorization:Bearer ${token}\n\n${payload}\0`;
    }
};