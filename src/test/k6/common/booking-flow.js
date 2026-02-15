// src/test/k6/common/booking-flow.js
import http from "k6/http";
import { check, sleep } from "k6";
import {
    holdTrend,
    holdFail,
    holdConflict,
    holdExpired,
    holdAuth,
    holdNotFound,
    holdServerErr,
    holdOk,
    holdRetries, holdTotalTrend,
} from "./metrics.js";

export function executeHoldWithRetry(baseUrl, basePaths, sessionId, ticketIds, authHeaders, maxRetries = 3) {
    const start = Date.now();

    let attempts = 0;
    let success = false;
    let lastRes = null;

    const body = JSON.stringify({
        sessionId: Number(sessionId),
        ticketIds,
    });

    const holdExpected = http.expectedStatuses(200, 409);

    while (attempts < maxRetries && !success) {
        attempts++;

        lastRes = http.post(`${baseUrl}${basePaths.hold}`, body, {
            ...authHeaders,
            tags: {
                ...authHeaders.tags,
            },
            responseCallback: holdExpected,
            responseType: "none", // body를 아예 읽지 않음(메모리/부하 감소)
        });

        if (lastRes.status === 200) {
            success = true;
            holdOk.add(1);
        } else if (lastRes.status === 409) {
            // 충돌 발생 - 재시도
            if (attempts < maxRetries) {
                sleep(0.2 + Math.random() * 0.4); // 재시도 대기
            }
        } else {
            // 다른 에러는 재시도 안함 (401, 404, 5xx 등)
            break;
        }
    }

    // 메트릭 기록 (마지막 응답 기준)
    if (lastRes) {
        holdTrend.add(lastRes.timings.duration);
        holdConflict.add(lastRes.status === 409);
        holdExpired.add(lastRes.status === 410);
        holdAuth.add(lastRes.status === 401 || lastRes.status === 403);
        holdNotFound.add(lastRes.status === 404);
        holdServerErr.add(lastRes.status >= 500);

        const ok = check(lastRes, {
            "hold status 200 or 409": (r) => r.status === 200 || r.status === 409,
        });

        holdFail.add(!ok);
    }

    holdRetries.add(attempts); // 재시도 횟수 기록

    const totalMs = Date.now() - start; // 전체 종료(재시도+sleep 포함)
    holdTotalTrend.add(totalMs);

    return { success, lastRes, attempts, totalMs };
}