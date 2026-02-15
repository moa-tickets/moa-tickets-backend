// booking-vu.js
// VU 테스트 (동시 사용자 폭을 보는 테스트)
// “10k VU가 떠 있는 동안 서버가 죽지 않는가”
// (단, 이건 RPS를 보장하지 않음. VU가 많아도 sleep/409/대기 때문에 실제 RPS는 낮을 수 있음)
// ramping-vus
// 태그는 name만
// performance 성격이면 SEAT_MODE=spread, HOT_POOL_SIZE 크게 / maxRetries=0~1

/*
BASE_URL=http://localhost:8080 \
SESSION_ID=70 \
MEMBER_ID_RANGE="1-20000" \
TICKET_ID_RANGE="11951-21950" \
SEAT_MODE=spread \
HOT_POOL_SIZE=500 \
TOKEN_POOL_SIZE=10000 \
THINK_TIME_SEC=1.0 \
MAX_RETRIES=0 \
k6 run src/test/k6/booking-vu.js
*/

import { group, sleep, fail } from "k6";
import { parseRange, parseCsv, parseJsonArray, resolveMemberSource, setupTokens, pickMemberIdForVu, pickTicketId } from "./common/utils.js";
import { executeHoldWithRetry } from "./common/booking-flow.js";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SESSION_ID = String(__ENV.SESSION_ID || "1");
const TEST_TOKEN_ENDPOINT = __ENV.TEST_TOKEN_ENDPOINT || "/test/auth/token";

const BASE_PATHS = { hold: `/api/tickets/hold` };

const MEMBER_RANGE = parseRange(__ENV.MEMBER_ID_RANGE || "");
const MEMBER_IDS = parseCsv(__ENV.MEMBER_IDS || "");
const TICKET_RANGE = parseRange(__ENV.TICKET_ID_RANGE || "");
const TICKET_IDS = parseJsonArray(__ENV.TICKET_IDS || "[]");

const SEAT_MODE = __ENV.SEAT_MODE || "spread";
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "200");     // spread면 크게
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "10000");
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || "1.0");
const MAX_ATTEMPTS = Math.max(1, Number(__ENV.MAX_ATTEMPTS || "1"));

export const options = {
    discardResponseBodies: false,

    scenarios: {
        booking_vu: {
            executor: "ramping-vus",
            startVUs: 0,
            stages: [
                { duration: "30s", target: 100 },
                { duration: "1m",  target: 1000 },
                { duration: "1m",  target: 5000 },
                { duration: "4m",  target: 10000 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
        },
    },

    thresholds: {
        http_req_failed: ["rate<0.01"],          // 네트워크/서버 다운 감지
        http_req_duration: ["p(95)<1000"],       // “서버 응답성”의 최소선 (빡세게 잡지 말고 시작)
        booking_hold_duration: ["p(95)<800"],    // 마지막 hold 1회
        // total_duration은 성능(VU)테스트에서는 굳이 강제하지 않는 편 추천(재시도/대기 섞임)
    },
};

export function setup() {
    const memberSource = resolveMemberSource(MEMBER_RANGE, MEMBER_IDS);
    return setupTokens(BASE_URL, TEST_TOKEN_ENDPOINT, memberSource, TOKEN_STRATEGY, TOKEN_POOL_SIZE);
}


export default function (data) {
    if (__VU === 1 && __ITER === 0) {
        console.log(`MAX_ATTEMPTS=${MAX_ATTEMPTS}, THINK_TIME_SEC=${THINK_TIME_SEC}`);
    }

    const memberId = pickMemberIdForVu(data.issuedMemberIds);
    const token = data.tokenMap[String(memberId)];
    if (!token) fail(`No token found for memberId=${memberId}`);

    const authHeaders = {
        headers: { Cookie: `Authorization=${token};`, "Content-Type": "application/json" },
        tags: { name: "POST /api/tickets/hold" },      // 고정 태그
    };

    const pickedTicketId = pickTicketId(SEAT_MODE, TICKET_RANGE, TICKET_IDS, HOT_POOL_SIZE);
    const ticketIds = pickedTicketId ? [pickedTicketId] : [];

    group("hold", () => {
        executeHoldWithRetry(BASE_URL, BASE_PATHS, SESSION_ID, ticketIds, authHeaders, MAX_ATTEMPTS);
    });

    sleep(THINK_TIME_SEC);
}
