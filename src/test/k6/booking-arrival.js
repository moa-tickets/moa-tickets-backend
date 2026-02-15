// booking-arrival.js
// arrival-rate 테스트 (처리량 한계 RPS 찾기)
// “초당 N 요청(hold 흐름)을 계속 넣었을 때, 어디서부터 p95/실패율/락/GC가 무너지는가”
// CCU 목표(예: 30,000명)를 “실제로 들어오는 요청률”로 환산해서 보는 테스트에 더 잘 맞음

/*
BASE_URL=http://localhost:8080 \
SESSION_ID=70 \
MEMBER_ID_RANGE="1-30000" \
TICKET_ID_RANGE="11951-21950" \
SEAT_MODE=spread \
HOT_POOL_SIZE=1000 \
TOKEN_POOL_SIZE=30000 \
MAX_RETRIES=0 \
k6 run src/test/k6/booking-arrival.js
* */

import { group, fail } from "k6";
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
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "500");
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "20000");

// arrival-rate 테스트는 “재시도”가 처리량 측정을 망가뜨리기 쉬움 → 보통 0 권장
const MAX_RETRIES = Number(__ENV.MAX_RETRIES || "0");

export const options = {
    discardResponseBodies: true,

    scenarios: {
        booking_rps: {
            executor: "ramping-arrival-rate",
            timeUnit: "1s",

            // 목표 RPS 램프업
            stages: [
                { duration: "30s", target: 200 },    // 200 iters/s
                { duration: "1m",  target: 1000 },
                { duration: "2m",  target: 3000 },
                { duration: "2m",  target: 6000 },
                { duration: "3m",  target: 10000 },
            ],

            // k6가 rate를 맞추기 위해 VU를 늘려서 쓰는 풀
            preAllocatedVUs: 2000,
            maxVUs: 20000,         // 로컬이면 너무 높으면 k6 자체가 먼저 죽을 수 있음 → 상황보고 조절
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<1000"],       // “처리량 한계” 탐색용 기본선
        booking_hold_duration: ["p(95)<800"],
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
        headers: { Cookie: `Authorization=${token};`, "Content-Type": "application/json" },
        tags: { name: "POST /api/tickets/hold" },
    };

    const pickedTicketId = pickTicketId(SEAT_MODE, TICKET_RANGE, TICKET_IDS, HOT_POOL_SIZE);
    const ticketIds = pickedTicketId ? [pickedTicketId] : [];

    group("hold", () => {
        executeHoldWithRetry(BASE_URL, BASE_PATHS, SESSION_ID, ticketIds, authHeaders, MAX_RETRIES);
    });
}
