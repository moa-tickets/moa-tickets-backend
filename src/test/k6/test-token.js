import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 10, duration: '10s' };

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MEMBER_ID = __ENV.MEMBER_ID || '1';

export function setup() {
    const res = http.get(`${BASE_URL}/test/auth/token?memberId=${MEMBER_ID}`);
    // 원인 파악용: setup에서만 1번 출력
    console.log(`[setup] status=${res.status} body=${res.body}`);

    check(res, { 'token issued': (r) => r.status === 200 });

    const token = res.json('token');
    return { token };
}

export default function (data) {
    const res = http.get(`${BASE_URL}/api/sessions/1/tickets`, {
        headers: { Cookie: `Authorization=${data.token};` },
    });

    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(1);
}
