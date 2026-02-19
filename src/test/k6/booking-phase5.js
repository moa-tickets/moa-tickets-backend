// src/test/k6/booking-phase5.js
//
// Phase 5: 결제 포함 통합 테스트
// 목표: VU 2,000, 전체 예매 플로우 검증
//
// 실행 방법:
// BASE_URL=http://localhost:8080 \
// SESSION_ID=70 \
// MEMBER_ID_RANGE="1-1000" \
// TICKET_ID_RANGE="11951-21950" \
// k6 run src/test/k6/booking-phase5.js

import http from "k6/http";
import { group, sleep, fail, check } from "k6";
import { parseRange, parseCsv, parseJsonArray, resolveMemberSource, setupTokens, pickMemberIdForVu, pickTicketId } from "./common/utils.js";
import { executeHoldWithRetry } from "./common/booking-flow.js";
import { endToEndDuration, prepareTrend, confirmTrend, prepareFail, confirmFail, prepareOk, confirmOk } from "./common/metrics.js";

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
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "1000");
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || "1.5");

const MEMBER_RANGE = parseRange(MEMBER_ID_RANGE);
const TICKET_RANGE = parseRange(TICKET_ID_RANGE);

// Phase 5 전용 옵션 - 결제 포함
export const options = {
    scenarios: {
        booking: {
            executor: "ramping-vus",
            startVUs: 0,
            stages: [
                { duration: "1m", target: 500 },
                { duration: "2m", target: 2000 },
                { duration: "3m", target: 2000 },
                { duration: "1m", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<1500"],
        booking_hold_duration: ["p(95)<800"],
        booking_e2e_duration: ["p(95)<3000"],
        booking_hold_5xx: ["rate<0.005"],
        booking_prepare_fail_rate: ["rate<0.01"],
        booking_confirm_fail_rate: ["rate<0.05"],
        booking_hold_auth_401_403: ["rate==0"],
        booking_hold_notfound_404: ["rate==0"],
    },
};

export function setup() {
    const memberSource = resolveMemberSource(MEMBER_RANGE, MEMBER_IDS);
    return setupTokens(BASE_URL, TEST_TOKEN_ENDPOINT, memberSource, TOKEN_STRATEGY, TOKEN_POOL_SIZE);
}

export default function (data) {
    const startTime = Date.now();

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

    let holdToken = null;

    // 1. Hold
    group("01_hold", () => {
        const { success, lastRes } = executeHoldWithRetry(BASE_URL, BASE_PATHS, SESSION_ID, ticketIds, authHeaders, 3);

        if (success && lastRes && lastRes.status === 200) {
            try {
                holdToken = lastRes.json("holdToken");
            } catch (e) {
                // holdToken 파싱 실패
            }
        }
    });

    if (!holdToken) {
        endToEndDuration.add(Date.now() - startTime);
        return; // hold 실패 시 결제 진행 안함
    }

    sleep(1); // 결제 생각 시간

    // 2. Payment Prepare
    group("02_payment_prepare", () => {
        const body = JSON.stringify({ holdToken });
        const res = http.post(`${BASE_URL}${BASE_PATHS.paymentPrepare}`, body, {
            ...authHeaders,
            tags: { ...authHeaders.tags, step: "prepare" },
        });

        prepareTrend.add(res.timings.duration);

        const ok = check(res, {
            "prepare status 200": (r) => r.status === 200,
        });

        prepareFail.add(!ok);
        if (ok) prepareOk.add(1);
    });

    sleep(0.5);

    // 3. Payment Confirm
    group("03_payment_confirm", () => {
        const body = JSON.stringify({
            holdToken,
            paymentKey: `test_payment_${Date.now()}`,
            amount: 10000,
        });

        const res = http.post(`${BASE_URL}${BASE_PATHS.paymentConfirm}`, body, {
            ...authHeaders,
            tags: { ...authHeaders.tags, step: "confirm" },
        });

        confirmTrend.add(res.timings.duration);

        const ok = check(res, {
            "confirm status 200": (r) => r.status === 200,
        });

        confirmFail.add(!ok);
        if (ok) confirmOk.add(1);
    });

    endToEndDuration.add(Date.now() - startTime);
    sleep(THINK_TIME_SEC);
}