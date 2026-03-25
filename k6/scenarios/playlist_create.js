import http from 'k6/http';
import { check, sleep } from 'k6';
import {BASE_URL} from '../utils/const.js'

export const options = {
    stages: [
        { duration: '30s', target: 100 }, // 30초 동안 100명까지 점진적 증가
        { duration: '1m', target: 100 },  // 1분 동안 100명 유지
        { duration: '20s', target: 0 },   // 종료
    ],
};

// 2. setup에서 리턴한 데이터(tokens)를 인자로 받음
export default function (tokens) {
    const myTokenObj = tokens[(__VU - 1) % tokens.length];

    const playlistParams = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${myTokenObj.accessToken}`,
            'X-XSRF-TOKEN': myTokenObj.xsrfToken,
            'Cookie': `REFRESH_TOKEN=${myTokenObj.refreshToken}; XSRF-TOKEN=${myTokenObj.xsrfToken}`,
        },
    };

    const playlistPayload = JSON.stringify({
        title: `테스트 플레이리스트 No.${__VU}-${__ITER}`,
        description: "Login once, create many!"
    });

    const createRes = http.post(`${BASE_URL.ALB}/api/playlists`, playlistPayload, playlistParams);

    check(createRes, {
        'playlist created successfully': (r) => r.status === 201 || r.status === 200,
    });

    sleep(1);
}