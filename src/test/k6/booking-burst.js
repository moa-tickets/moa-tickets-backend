// booking-burst.js
// “짧은 시간(예: 10~30초)에 갑자기 엄청난 요청이 몰릴 때”
// LB/커넥션/스레드풀/DB 풀/락이 어떻게 터지는지 확인
// ramping-arrival-rate로 “평상시 → 급폭발 → 평상시” 만들기

/*
BASE_URL=http://localhost:8080 \
SESSION_ID=70 \
MEMBER_ID_RANGE="1-30000" \
TICKET_ID_RANGE="11951-21950" \
SEAT_MODE=spread \
HOT_POOL_SIZE=2000 \
TOKEN_POOL_SIZE=30000 \
MAX_RETRIES=0 \
k6 run src/test/k6/booking-burst.js

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
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "1000");
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "30000");
const MAX_RETRIES = Number(__ENV.MAX_RETRIES || "0");

export const options = {
    discardResponseBodies: true,

    scenarios: {
        burst: {
            executor: "ramping-arrival-rate",
            timeUnit: "1s",
            stages: [
                { duration: "1m",  target: 500 },     // 워밍업
                { duration: "10s", target: 15000 },   // 🔥 버스트
                { duration: "50s", target: 2000 },    // 버스트 후 안정화
                { duration: "1m",  target: 500 },
            ],
            preAllocatedVUs: 5000,
            maxVUs: 30000,
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<1500"],     // burst는 기준 완화하고 “서버 다운/리셋/타임아웃”을 더 중점 관찰
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
