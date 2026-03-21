/**
* 랜덤 값 생성, 정규식 추출 등 반복되는 작업을 대신 해줌
* */

// 배열에서 랜덤하게 하나를 뽑기
export function getRandomElement(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// 지정된 범위 내의 랜덤한 대기 시간(ms)을 반환
export function getRandomDelay(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}