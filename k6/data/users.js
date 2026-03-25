import { SharedArray } from 'k6/data';

// mopl_users.csv 파일에서 유저 정보(username)를 읽어와 공유 배열로 반환
export const users = new SharedArray('users', function () {
    const file = open('./users.csv');
    const lines = file.split('\n');

    return lines.slice(1) // 첫 번째 줄(헤더) 제외
        .filter(line => line.trim() !== '')
        .map(line => line.split(',')[2].replace(/"/g, '')); // 3번째 컬럼(email) 추출 및 따옴표 제거
});