// src/test/k6/booking-phase0.js
//
// Phase 0: 베이스라인 측정
// 목표: VU 100~500, 시스템 기본 성능 파악
//
// 실행 방법:
// BASE_URL=http://localhost:8080 \
// SESSION_ID=70 \
// MEMBER_ID_RANGE="1-500" \
// TICKET_ID_RANGE="11951-21950" \
// k6 run src/test/k6/booking-phase0.js

import http from "k6/http";
import { group, sleep, fail } from "k6";
import { parseRange, parseCsv, parseJsonArray, resolveMemberSource, setupTokens, pickMemberIdForVu, pickTicketId } from "./common/utils.js";
import { executeHoldWithRetry } from "./common/booking-flow.js";

// 환경변수
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

const SEAT_MODE = __ENV.SEAT_MODE || "spread";
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "50");
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "500");
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || "2.0");

const MEMBER_RANGE = parseRange(MEMBER_ID_RANGE);
const TICKET_RANGE = parseRange(TICKET_ID_RANGE);

// Phase 0 전용 옵션
export const options = {
    scenarios: {
        booking: {
            executor: "ramping-vus",
            startVUs: 0,
            stages: [
                { duration: "30s", target: 100 },
                { duration: "1m", target: 1000 },
                { duration: "5m",  target: 1000 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<800"],
        booking_hold_duration: ["p(95)<500"], // 마지막 hold 요청에 걸리는 응답 시간
        booking_hold_total_duration: ["p(95)<1500"], // 전체 hold 요청에 걸리는 총 응답 시간 (booking-flow 내부 sleep 포함 0.2~0.6초)
        booking_hold_5xx: ["rate<0.001"],
        booking_hold_auth_401_403: ["rate==0"],
        booking_hold_notfound_404: ["rate==0"],
        booking_hold_expired_410: ["rate==0"],
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
        executeHoldWithRetry(BASE_URL, BASE_PATHS, SESSION_ID, ticketIds, authHeaders, 3);
    });

    sleep(THINK_TIME_SEC);
}