// src/test/k6/booking-phase3.js
//
// Phase 3: 최악의 시나리오 (Stress Test)
// 목표: VU 10,000~15,000, 시스템 한계 확인
//
// 실행 방법:
// BASE_URL=http://localhost:8080 \
// SESSION_ID=70 \
// MEMBER_ID_RANGE="1-5000" \
// TICKET_ID_RANGE="11951-21950" \
// SEAT_MODE=hot \
// HOT_POOL_SIZE=50 \
// k6 run src/test/k6/booking-phase3.js

import { group, sleep, fail } from "k6";
import { parseRange, parseCsv, parseJsonArray, resolveMemberSource, setupTokens, pickMemberIdForVu, pickTicketId } from "./common/utils.js";
import { executeHoldWithRetry } from "./common/booking-flow.js";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SESSION_ID = String(__ENV.SESSION_ID || "1");
const TEST_TOKEN_ENDPOINT = __ENV.TEST_TOKEN_ENDPOINT || "/test/auth/token";

const BASE_PATHS = {
    tickets: `/api/sessions/${SESSION_ID}/tickets`,
    hold: `/api/tickets/hold`,
    paymentPrepare: `/api/payments/prepare`,
    paymentConfirm: `/api/payments/confirm`,
};

const MEMBER_ID_RANGE = __ENV.MEMBER_ID_RANGE || "";
const MEMBER_IDS = parseCsv(__ENV.MEMBER_IDS || "");
const TICKET_ID_RANGE = __ENV.TICKET_ID_RANGE || "";
const TICKET_IDS = parseJsonArray(__ENV.TICKET_IDS || "[]");

const SEAT_MODE = __ENV.SEAT_MODE || "hot"; // Phase 3에서는 hot 권장
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "50");
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "5000");
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || "0.3"); // 짧은 think time으로 부하 증가

const MEMBER_RANGE = parseRange(MEMBER_ID_RANGE);
const TICKET_RANGE = parseRange(TICKET_ID_RANGE);

// Phase 3 전용 옵션 - 느슨한 threshold
export const options = {
    scenarios: {
        booking: {
            executor: "ramping-vus",
            startVUs: 0,
            stages: [
                { duration: "1m", target: 5000 },   // 빠른 증가
                { duration: "2m", target: 10000 },  // 10k
                { duration: "2m", target: 10000 },  // 유지
                { duration: "1m", target: 15000 },  // 15k (최대 부하)
                { duration: "2m", target: 15000 },  // 유지
                { duration: "1m", target: 0 },      // 램프다운
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<3000"], // 느슨하게
        booking_hold_duration: ["p(95)<2000"],
        booking_hold_5xx: ["rate<0.1"], // 10%까지 허용
        booking_hold_auth_401_403: ["rate==0"],
        booking_hold_notfound_404: ["rate==0"],
        booking_hold_expired_410: ["rate<0.05"],
        booking_hold_conflict_409: ["rate<0.8"], // 80%까지 허용 (경합 극심)
    },
};

export function setup() {
    const memberSource = resolveMemberSource(MEMBER_RANGE, MEMBER_IDS);
    return setupTokens(BASE_URL, TEST_TOKEN_ENDPOINT, memberSource, TOKEN_STRATEGY, TOKEN_POOL_SIZE);
}

export default function (data) {
    const memberId = pickMemberIdForVu(data.issuedMemberIds);
    const token = data.tokenMap[String(memberId)];
    if (!token) fail(`No token found for memberId=${memberId}`);

    const authHeaders = {
        headers: {
            Cookie: `Authorization=${token};`,
            "Content-Type": "application/json",
        },
        tags: {
            memberId: String(memberId),
            sessionId: String(SESSION_ID),
        },
    };

    const pickedTicketId = pickTicketId(SEAT_MODE, TICKET_RANGE, TICKET_IDS, HOT_POOL_SIZE);
    const ticketIds = pickedTicketId ? [pickedTicketId] : [];

    group("01_hold", () => {
        executeHoldWithRetry(BASE_URL, BASE_PATHS, SESSION_ID, ticketIds, authHeaders, 5); // 재시도 5회
    });

    sleep(THINK_TIME_SEC);
}