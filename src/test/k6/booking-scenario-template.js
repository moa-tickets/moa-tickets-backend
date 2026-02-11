// src/test/k6/booking_scenario_template.js
//
// 목적:
// - 예매(좌석 홀드) 부하테스트 템플릿 (결제 단계는 옵션)
// - /test/auth/token으로 토큰을 setup에서 "1번만" 발급
// - member / ticket 을 range 기반으로 받아 대규모(수만) 테스트에 적합
//
// 사용 예시(권장: range):
// BASE_URL="http://localhost:8080" \
// SESSION_ID="68" \
// MEMBER_ID_RANGE="1-20000" \
// TICKET_ID_RANGE="5951-8950" \
// SEAT_MODE="hot" \
// HOT_POOL_SIZE="50" \
// ENABLE_PAYMENT="0" \
// k6 run src/test/k6/booking_scenario_template.js
//
// 사용 예시(옵션: list fallback):
// MEMBER_IDS="1,2,3,4" \
// TICKET_IDS='[5951,5952,5953,5954]' \
// k6 run ...

import http from "k6/http";
import { check, group, sleep, fail } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";

// -----------------------------
// 커스텀 메트릭
// -----------------------------
const holdTrend = new Trend("booking_hold_duration");

const holdFail = new Rate("booking_hold_fail_rate");
const holdConflict = new Rate("booking_hold_conflict_409");
const holdExpired = new Rate("booking_hold_expired_410");
const holdAuth = new Rate("booking_hold_auth_401_403");
const holdNotFound = new Rate("booking_hold_notfound_404");
const holdServerErr = new Rate("booking_hold_5xx");

const holdOk = new Counter("booking_hold_ok");

// (옵션) 결제 메트릭 자리만 남겨둠
const prepareTrend = new Trend("booking_prepare_duration");
const confirmTrend = new Trend("booking_confirm_duration");
const prepareFail = new Rate("booking_prepare_fail_rate");
const confirmFail = new Rate("booking_confirm_fail_rate");
const prepareOk = new Counter("booking_prepare_ok");
const confirmOk = new Counter("booking_confirm_ok");

// -----------------------------
// 옵션 (부하 패턴)
// -----------------------------
export const options = {
    scenarios: {
        booking: {
            executor: "ramping-vus",
            startVUs: Number(__ENV.START_VUS || "2"),
            stages: [
                { duration: __ENV.WARMUP_DURATION || "30s", target: Number(__ENV.WARMUP_TARGET || "5") },
                { duration: __ENV.RAMP1_DURATION || "1m", target: Number(__ENV.RAMP1_TARGET || "20") },
                { duration: __ENV.PEAK_DURATION || "1m", target: Number(__ENV.PEAK_TARGET || "50") },
                { duration: __ENV.RAMPDOWN_DURATION || "30s", target: Number(__ENV.RAMPDOWN_TARGET || "0") },
            ],
            gracefulRampDown: __ENV.GRACEFUL_RAMPDOWN || "30s",
            gracefulStop: __ENV.GRACEFUL_STOP || "30s",
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<800"],

        // ✅ 홀드에서 409는 정상적인 "경쟁 실패"로 볼 수 있으니
        // - 체크는 200/409를 성공으로 취급
        // - 진짜 장애(5xx, 401/403, 404 등)만 따로 감시
        booking_hold_duration: ["p(95)<800"],
        booking_hold_5xx: ["rate<0.005"],
        booking_hold_auth_401_403: ["rate==0"],
        booking_hold_notfound_404: ["rate==0"],
        booking_hold_expired_410: ["rate==0"],
    },
};

// -----------------------------
// 환경변수
// -----------------------------
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SESSION_ID = String(__ENV.SESSION_ID || "1");

const TEST_TOKEN_ENDPOINT = __ENV.TEST_TOKEN_ENDPOINT || "/test/auth/token";

const BASE_PATHS = {
    tickets: `/api/sessions/${SESSION_ID}/tickets`,
    hold: `/api/tickets/hold`,
    paymentPrepare: `/api/payments/prepare`,
    paymentConfirm: `/api/payments/confirm`,
};

const ENABLE_PAYMENT = (__ENV.ENABLE_PAYMENT || "0") === "1";

// member 입력: range 우선, 없으면 list fallback
const MEMBER_ID_RANGE = __ENV.MEMBER_ID_RANGE || ""; // e.g. "1-20000"
const MEMBER_IDS = parseCsv(__ENV.MEMBER_IDS || ""); // e.g. "1,2,3"

// ticket 입력: range 우선, 없으면 list fallback
const TICKET_ID_RANGE = __ENV.TICKET_ID_RANGE || ""; // e.g. "5951-8950"
const TICKET_IDS = parseJsonArray(__ENV.TICKET_IDS || "[]");

// seat pick mode
const SEAT_MODE = __ENV.SEAT_MODE || "hot"; // hot | spread
const HOT_POOL_SIZE = Number(__ENV.HOT_POOL_SIZE || "50"); // hot일 때, range의 앞 N개만 집중

// token 발급/캐시 전략
const TOKEN_STRATEGY = __ENV.TOKEN_STRATEGY || "pool";
// "pool": setup에서 지정 개수만큼만 발급해 토큰 풀로 돌려씀(대규모에 권장)
// "all": range 전체(예: 1~20000) 토큰을 전부 발급 (로컬에선 비추)

const TOKEN_POOL_SIZE = Number(__ENV.TOKEN_POOL_SIZE || "500");
// TOKEN_STRATEGY=pool일 때 setup에서 발급할 토큰 개수(최대 부하 유저 수보다 넉넉히)

// think time
const THINK_TIME_SEC = Number(__ENV.THINK_TIME_SEC || "0.5");

// -----------------------------
// range 파싱 유틸
// -----------------------------
function parseRange(rangeStr) {
    const m = String(rangeStr).trim().match(/^(\d+)\s*-\s*(\d+)$/);
    if (!m) return null;
    const min = Number(m[1]);
    const max = Number(m[2]);
    if (!Number.isFinite(min) || !Number.isFinite(max) || min <= 0 || max < min) return null;
    return { min, max };
}

const MEMBER_RANGE = parseRange(MEMBER_ID_RANGE);
const TICKET_RANGE = parseRange(TICKET_ID_RANGE);

// -----------------------------
// setup: 토큰 발급
// -----------------------------
export function setup() {
    const memberSource = resolveMemberSource();
    if (!memberSource) {
        fail("No member source. Provide MEMBER_ID_RANGE=\"a-b\" or MEMBER_IDS=\"1,2,3\".");
    }

    // 어떤 memberId들에 대해 토큰을 발급할지 결정
    const idsToIssue = pickMembersForTokenIssue(memberSource);

    const tokenMap = {}; // memberId -> token
    for (const memberId of idsToIssue) {
        const url = `${BASE_URL}${TEST_TOKEN_ENDPOINT}?memberId=${memberId}`;
        const res = http.get(url, { tags: { step: "token_issue" } });

        const ok = check(res, {
            "token issue status 200": (r) => r.status === 200,
            "token field exists": (r) => !!safeJson(r, "token"),
        });

        if (!ok) {
            fail(`Token issue failed for memberId=${memberId}: status=${res.status}, body=${res.body}`);
        }

        tokenMap[String(memberId)] = res.json("token");
    }

    return {
        tokenMap,
        issuedMemberIds: Object.keys(tokenMap).map((x) => Number(x)),
    };
}

function resolveMemberSource() {
    if (MEMBER_RANGE) return { type: "range", range: MEMBER_RANGE };
    if (MEMBER_IDS.length > 0) return { type: "list", list: MEMBER_IDS };
    return null;
}

function pickMembersForTokenIssue(memberSource) {
    if (memberSource.type === "list") {
        return memberSource.list;
    }

    // range
    const { min, max } = memberSource.range;
    const total = max - min + 1;

    if (TOKEN_STRATEGY === "all") {
        // ⚠️ 2만명 토큰 발급은 setup이 오래 걸릴 수 있음
        const out = [];
        for (let i = 0; i < total; i++) out.push(min + i);
        return out;
    }

    // TOKEN_STRATEGY=pool
    const size = Math.min(TOKEN_POOL_SIZE, total);
    // range 앞쪽부터 size개를 발급 (원하면 랜덤 샘플링도 가능)
    const out = [];
    for (let i = 0; i < size; i++) out.push(min + i);
    return out;
}

// -----------------------------
// VU 반복 로직
// -----------------------------
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

    const pickedTicketId = pickTicketId();
    const ticketIds = pickedTicketId ? [pickedTicketId] : [];

    group("01_hold", () => {
        const body = JSON.stringify({
            sessionId: Number(SESSION_ID),
            ticketIds,
        });

        const holdExpected = http.expectedStatuses(200, 409);

        const res = http.post(`${BASE_URL}${BASE_PATHS.hold}`, body, {
            ...authHeaders,
            tags: { ...authHeaders.tags, step: "hold" },
            responseCallback: holdExpected,
        });

        holdTrend.add(res.timings.duration);

        // ✅ 200=성공, 409=경쟁으로 실패(정상 시나리오) → 둘 다 ok
        const ok = check(res, {
            "hold status 200 or 409": (r) => r.status === 200 || r.status === 409,
        });

        holdFail.add(!ok);

        holdConflict.add(res.status === 409);
        holdExpired.add(res.status === 410);
        holdAuth.add(res.status === 401 || res.status === 403);
        holdNotFound.add(res.status === 404);
        holdServerErr.add(res.status >= 500);

        if (!ok) return;

        holdOk.add(1);

        if (!ENABLE_PAYMENT) return;

        // 결제 단계는 필요 시 여기에 이어서 붙이면 됨
        // (지금은 hold-only 집중이 목표라면 ENABLE_PAYMENT=0 권장)
    });

    sleep(THINK_TIME_SEC);
}

// -----------------------------
// member/ticket pick
// -----------------------------
function pickMemberIdForVu(issuedMemberIds) {
    // setup에서 발급된 토큰 풀 내에서만 선택
    // VU별로 고정 매핑하면 재현성이 좋아짐
    if (!issuedMemberIds || issuedMemberIds.length === 0) fail("No issuedMemberIds in setup data");
    const idx = (__VU - 1) % issuedMemberIds.length;
    return issuedMemberIds[idx];
}

function pickTicketId() {
    // 1) hot mode: range가 있으면 앞 HOT_POOL_SIZE만 집중
    if (SEAT_MODE === "hot" && TICKET_RANGE) {
        const min = TICKET_RANGE.min;
        const max = Math.min(TICKET_RANGE.max, min + HOT_POOL_SIZE - 1);
        return min + Math.floor(Math.random() * (max - min + 1));
    }

    // 2) spread: range 전체
    if (TICKET_RANGE) {
        return TICKET_RANGE.min + Math.floor(Math.random() * (TICKET_RANGE.max - TICKET_RANGE.min + 1));
    }

    // 3) fallback: 배열
    if (TICKET_IDS.length > 0) {
        if (SEAT_MODE === "hot") {
            const pool = TICKET_IDS.slice(0, Math.min(Number(__ENV.HOT_POOL || "5"), TICKET_IDS.length));
            return pool[Math.floor(Math.random() * pool.length)];
        }
        return TICKET_IDS[Math.floor(Math.random() * TICKET_IDS.length)];
    }

    return null;
}

// -----------------------------
// 파서/유틸
// -----------------------------
function parseCsv(s) {
    if (!s) return [];
    return String(s)
        .split(",")
        .map((x) => x.trim())
        .filter(Boolean)
        .map((x) => Number(x))
        .filter((x) => Number.isFinite(x) && x > 0);
}

function parseJsonArray(s) {
    try {
        const v = JSON.parse(String(s));
        return Array.isArray(v) ? v.map((x) => Number(x)).filter((n) => Number.isFinite(n) && n > 0) : [];
    } catch {
        return [];
    }
}

function safeJson(res, path) {
    try {
        const obj = res.json();
        if (!obj) return null;
        const parts = String(path).split(".");
        let cur = obj;
        for (const p of parts) {
            if (cur == null) return null;
            cur = cur[p];
        }
        return cur ?? null;
    } catch {
        return null;
    }
}
