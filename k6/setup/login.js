import http from 'k6/http';
import { sleep } from 'k6';
import { users } from '../data/users.js';
import {BASE_URL} from '../utils/const.js'

export function setupLogin() {
    const tokens = [];
    const batchSize = 100; // 한 번에 100명씩 병렬 로그인
    const loginCount = 200; // 총 로그인할 유저 수 (필요에 따라 조절)

    console.log(`[Setup] ${loginCount}명 로그인 시작...`);

    for (let i = 0; i < loginCount; i += batchSize) {
        const batchRequests = [];

        for (let j = i; j < i + batchSize && j < loginCount; j++) {
            // users 배열에서 username을 순차적으로 가져옵니다.
            const email = users[j % users.length];

            batchRequests.push({
                method: 'POST',
                url: `${BASE_URL.ALB}/api/auth/sign-in`,
                body: {
                    username: email,
                    password: 'mopl1234',
                },
                params: {
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
                },
            });
        }

        // 2. http.batch로 병렬 요청 실행
        const responses = http.batch(batchRequests);

        // 3. 결과 수집 및 3가지 토큰 추출
        responses.forEach((res) => {
            if (res.status === 200) {
                try {
                    const body = res.json();
                    const cookies = res.cookies;

                    // 3개의 토큰 추출 로직
                    const accessToken = body.accessToken;
                    const xsrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0].value : '';
                    const refreshToken = cookies['REFRESH_TOKEN'] ? cookies['REFRESH_TOKEN'][0].value : '';

                    // accessToken이 정상적으로 발급된 경우에만 배열에 추가
                    if (accessToken) {
                        tokens.push({
                            accessToken: accessToken,
                            refreshToken: refreshToken,
                            xsrfToken: xsrfToken,
                        });
                    }
                } catch (e) {
                    console.error("토큰 추출 실패", e);
                }
            }
        });

        console.log(`Progress: ${tokens.length} 사용자 token 수집...`);
        sleep(1); // 서버 부하 조절을 위해 배치 사이의 짧은 휴식
    }

    if (tokens.length === 0) {
        throw new Error("로그인 실패: 추출된 토큰이 없습니다.");
    }

    // [{ accessToken, refreshToken, xsrfToken}, { ... }, ...] 형태의 배열 반환
    return tokens;
}