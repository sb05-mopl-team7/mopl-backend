import http from 'k6/http';
import { check, sleep } from 'k6';
import {BASE_URL} from "../utils/const.js";

/**
* 트래픽이 점진적으로 증가하는 상황에서
* 신규 유저 회원가입 API(/api/users)의 응답 속도 및 데이터베이스(DB) 인서트 성능을 검증
* */
export const options = {
    stages: [
        { duration: '30s', target: 20 }, // 30초 동안 20명
        { duration: '1m', target: 100 },  // 1분 동안 100명까지 증가
        { duration: '30s', target: 0 },    // 종료
    ],
};

export default function () {

    const email = `mopl-user-${__VU}-${__ITER}@test.com`;

    const payload = JSON.stringify({
        email: email,
        password: 'mopl1234',
        name: `사용자${__VU}-${__ITER}`
    });

    const params = { headers: { 'Content-Type': 'application/json' } };

    const res = http.post(`${BASE_URL.ALB}/api/users`, payload, params);

    check(res, {
        'signup success': (r) => r.status === 201 || r.status === 200,
    });

    sleep(1);
}