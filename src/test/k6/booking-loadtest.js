// src/test/k6/booking_scenario_template.js
//
// 목적:
// - "예매(좌석 홀드 → (선택) 결제 준비 → (선택) 결제 확정/완료)" 형태의 부하테스트 템플릿
// - 테스트 환경에서만 열리는 /test/auth/token으로 토큰을 "테스트 시작 시 1번만" 발급(setup)
// - 이후 단계별로 부하를 점점 세게 걸면서 Grafana 캡처/기록에 쓰기 좋게 메트릭/태그/threshold 포함
//
// 사용 방법(예시):
// BASE_URL="http://localhost:8080" \
// SESSION_ID="1" \
// MEMBER_IDS="1,2,3,4,5,6,7,8,9,10" \
// SEAT_IDS="[101,102,103,104,105]" \
// k6 run src/test/k6/booking_scenario_template.js
//
// 엔드포인트는 너희 프로젝트에 맞게 아래 BASE_PATHS만 수정하면 됨.

import http from "k6/http";
import { check, group, sleep, fail } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";

// -----------------------------
// 커스텀 메트릭 (Grafana에서 보기 좋게)
// -----------------------------
const holdTrend = new Trend("booking_hold_duration"); // hold API 응답시간
const prepareTrend = new Trend("booking_prepare_duration"); // payment prepare
const confirmTrend = new Trend("booking_confirm_duration"); // payment confirm/finalize

const holdFail = new Rate("booking_hold_fail_rate");
const prepareFail = new Rate("booking_prepare_fail_rate");
const confirmFail = new Rate("booking_confirm_fail_rate");

const holdConflict = new Rate("booking_hold_conflict_409");   // 경쟁/정책 충돌(정상일 수 있음)
const holdExpired = new Rate("booking_hold_expired_410");     // 홀드 만료(hold-only면 보통 0이 이상적)
const holdAuth = new Rate("booking_hold_auth_401_403");       // 인증/권한 문제
const holdNotFound = new Rate("booking_hold_notfound_404");   // 데이터/세션/티켓 불일치
const holdServerErr = new Rate("booking_hold_5xx");           // 진짜 장애

const holdOk = new Counter("booking_hold_ok");
const prepareOk = new Counter("booking_prepare_ok");
const confirmOk = new Counter("booking_confirm_ok");



// -----------------------------
// 옵션: 점점 강하게 부하를 거는 "stages" 템플릿
// - 처음엔 작게(스모크), 그다음 서서히 상승, 피크 유지, 하강
// -----------------------------
export const options = {
    scenarios: {
        booking: {
            executor: "ramping-vus",
            startVUs: 2,
            stages: [
                { duration: "30s", target: 5 }, // 워밍업 (서버가 껐다 켜져도 실제 유저가 있었던 상황 반영)
                { duration: "1m", target: 20 },   // 1차 상승
                { duration: "2m", target: 50 },   // 2차 상승
                { duration: "1m", target: 50 },   // 피크 유지(캡처 타이밍)
                { duration: "30s", target: 0 },   // 종료
            ],
            gracefulRampDown: "30s",
        },
    },

    thresholds: {
        // 전체 HTTP 기본 지표
        // http_req_failed: ["rate<0.02"],          // 실패율 2% 미만(초기 기준)
        http_req_duration: ["p(95)<800"],        // p95 800ms 미만(초기 기준)

        // 단계별로 분리한 지표(캡처/원인 분석에 도움)
        booking_hold_fail_rate: ["rate<0.05"],       // 경쟁 실패도 포함
        booking_hold_duration: ["p(95)<800"],        // 기준선 (나중에 튜닝하며 낮추기)
        booking_hold_5xx: ["rate<0.005"],            // 0.5% 미만
        booking_hold_auth_401_403: ["rate==0"],      // 0이어야 정상
        booking_hold_notfound_404: ["rate==0"],      // 0이어야 정상

        // 결제 단계는(있다면) 더 느릴 수 있으니 기준을 완화해서 시작
        // booking_prepare_fail_rate: ["rate<0.10"],
        // booking_prepare_duration: ["p(95)<1500"],
        // booking_confirm_fail_rate: ["rate<0.10"],
        // booking_confirm_duration: ["p(95)<2000"],
    },
};

// -----------------------------
// 환경변수
// -----------------------------
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SESSION_ID = __ENV.SESSION_ID || "1";

// 토큰 발급용 (테스트 환경 전용)
const TEST_TOKEN_ENDPOINT = __ENV.TEST_TOKEN_ENDPOINT || "/test/auth/token";

// “예매 플로우” 엔드포인트 (프로젝트에 맞게 수정)
const BASE_PATHS = {
    // 1) 좌석/티켓 조회(선택): 부하 전 사전 확인 또는 테스트 데이터 확인에 사용 가능
    tickets: `/api/sessions/${SESSION_ID}/tickets`,

    // 2) 좌석 홀드(예매 시작)
    hold: `/api/tickets/hold`, // 예: POST /api/tickets/hold

    // 3) 결제 준비(선택)
    paymentPrepare: `/api/payments/prepare`,

    // 4) 결제 확정/완료(선택)
    paymentConfirm: `/api/payments/confirm`,
};

// 테스트 계정 풀 (콤마 구분)
const MEMBER_IDS = parseCsv(__ENV.MEMBER_IDS || "1");
const MEMBER_ID_START = Number(__ENV.MEMBER_ID_START || "0");
const MEMBER_ID_END = Number(__ENV.MEMBER_ID_END || "0");

// 시나리오에서 사용할 seat/ticket id 목록 (JSON 배열)
const SEAT_IDS = parseJsonArray(__ENV.SEAT_IDS || "[]");

// “결제” 단계에서 필요할 수 있는 고정 파라미터들 (프로젝트에 맞게 수정)
const AMOUNT = Number(__ENV.AMOUNT || "1000");

// 옵션: 결제 단계를 포함할지 여부 (0/1)
const ENABLE_PAYMENT = (__ENV.ENABLE_PAYMENT || "0") === "1";

const SEAT_MODE = __ENV.SEAT_MODE || "hot"; // hot | spread
const HOT_POOL = Number(__ENV.HOT_POOL || "5");

// -----------------------------
// setup: 테스트 시작 시 1번만 실행
// - memberId 목록에 대해 토큰을 미리 발급(권장: 적당히 적은 수)
// - k6의 setup 데이터는 VU들이 공유(읽기 전용)
// -----------------------------
export function setup() {
    const memberIds = buildMemberIds();
    if (MEMBER_IDS.length === 0) fail("MEMBER_IDS is empty");

    const tokenMap = {}; // memberId -> jwt

    for (const memberId of MEMBER_IDS) {
        const url = `${BASE_URL}${TEST_TOKEN_ENDPOINT}?memberId=${memberId}`;
        const res = http.get(url, { tags: { step: "token_issue" } });

        const ok = check(res, {
            "token issue status 200": (r) => r.status === 200,
            "token field exists": (r) => !!safeJson(r, "token"),
        });

        if (!ok) {
            // 토큰 발급이 안 되면 부하테스트 의미가 없으니 즉시 종료
            fail(`Token issue failed for memberId=${memberId}: status=${res.status}, body=${res.body}`);
        }

        tokenMap[String(memberId)] = res.json("token");
    }

    return { tokenMap, memberIds };
}

// -----------------------------
// default: VU 반복 로직
// - VU마다 memberId를 고정 매핑(테스트 재현성↑)
// - seatId는 env로 준 목록에서 랜덤 선택(동시성 충돌 유도)
// -----------------------------
export default function (data) {
    const memberId = pickMemberForVu(data.memberIds);
    const token = data.tokenMap[String(memberId)];
    if (!token) fail(`No token found for memberId=${memberId}`);

    const authHeaders = {
        headers: {
            Cookie: `Authorization=${token};`, // 너희 인증 방식(쿠키 Authorization)
            "Content-Type": "application/json",
        },
        tags: {
            memberId: String(memberId),
            sessionId: String(SESSION_ID),
        },
    };

    const picked = pickTicketId(SEAT_IDS);
    const ticketIds = picked ? [picked] : [];

    group("01_hold", () => {
        const body = JSON.stringify({
            sessionId: Number(SESSION_ID),
            ticketIds,
        });

        const res = http.post(`${BASE_URL}${BASE_PATHS.hold}`, body, {
            ...authHeaders,
            tags: { ...authHeaders.tags, step: "hold" },
        });

        holdTrend.add(res.timings.duration);

        // 409도 성공처리(이미 다른 사용자가 선점한 좌석)
        const ok = check(res, {
            "hold status 200, 409": (r) => r.status === 200 || r.status === 409,
        });

        holdFail.add(!ok);
        holdExpired.add(res.status === 410);
        holdAuth.add(res.status === 401 || res.status === 403);
        holdNotFound.add(res.status === 404);
        holdServerErr.add(res.status >= 500);
        holdConflict.add(res.status === 409);

        if (!ok) {
            return;
        }

        holdOk.add(1);


        // 결제 단계는 선택 (ENABLE_PAYMENT=1 일 때만)
        if (!ENABLE_PAYMENT) return;

    });

    // 유저 think time (실서비스스럽게)
    sleep(0.5);
}

// -----------------------------
// 유틸
// -----------------------------
function parseCsv(s) {
    return String(s)
        .split(",")
        .map((x) => x.trim())
        .filter(Boolean)
        .map((x) => Number(x));
}

function parseJsonArray(s) {
    try {
        const v = JSON.parse(String(s));
        return Array.isArray(v) ? v : [];
    } catch {
        return [];
    }
}

function pickMemberForVu(memberIds) {
    // VU는 1부터 시작. VU마다 계정을 고정하면 “재현성”이 좋아짐.
    const idx = (__VU - 1) % memberIds.length;
    return memberIds[idx];
}

function randomPick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

function safeJson(res, path) {
    // path 예: "token", "data.holdToken"
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

function pickTicketId(seatIds) {
    if (seatIds.length === 0) return null;
    const pool = (SEAT_MODE === "hot") ? seatIds.slice(0, Math.min(HOT_POOL, seatIds.length)) : seatIds;
    return pool[Math.floor(Math.random() * pool.length)];
}

function buildMemberIds() {
    if (MEMBER_IDS.length > 0) {
        return MEMBER_IDS;
    }

    if (MEMBER_ID_START > 0 && MEMBER_ID_END >= MEMBER_ID_START) {
        const arr = [];
        for (let i = MEMBER_ID_START; i <= MEMBER_ID_END; i++) {
            arr.push(i);
        }
        return arr;
    }

    fail("Either MEMBER_IDS or MEMBER_ID_START/END must be provided");
}
