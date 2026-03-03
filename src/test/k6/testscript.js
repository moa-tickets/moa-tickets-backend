// src/test/k6/testscript.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export const unauthorized = new Rate('unauthorized');
export const forbidden = new Rate('forbidden');
export const other_errors = new Rate('other_errors');

export const options = {
    vus: 10,
    duration: '10s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        // 필요하면 확인
        unauthorized: ['rate==0'],
        forbidden: ['rate==0'],
        other_errors: ['rate==0'],
    },
};

const AUTH = __ENV.AUTH || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const url = `${BASE_URL}/api/sessions/1/tickets`;

    const params = {
        headers: {
            Cookie: `Authorization=${AUTH};`,
            Origin: BASE_URL,
        },
    };

    const res = http.get(url, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 로그는 딱 1번만 (첫 VU, 첫 iteration)
    if (res.status !== 200 && __VU === 1 && __ITER === 0) {
        console.log(`status=${res.status}, body=${res.body.slice(0, 200)}`);
    }

    unauthorized.add(res.status === 401);
    forbidden.add(res.status === 403);
    other_errors.add(res.status !== 200 && res.status !== 401 && res.status !== 403);

    sleep(1);
}
