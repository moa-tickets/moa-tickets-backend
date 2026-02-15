// src/test/k6/common/utils.js
import http from "k6/http";
import { check, fail } from "k6";

export function parseRange(rangeStr) {
    const m = String(rangeStr).trim().match(/^(\d+)\s*-\s*(\d+)$/);
    if (!m) return null;
    const min = Number(m[1]);
    const max = Number(m[2]);
    if (!Number.isFinite(min) || !Number.isFinite(max) || min <= 0 || max < min) return null;
    return { min, max };
}

export function parseCsv(s) {
    if (!s) return [];
    return String(s)
        .split(",")
        .map((x) => x.trim())
        .filter(Boolean)
        .map((x) => Number(x))
        .filter((x) => Number.isFinite(x) && x > 0);
}

export function parseJsonArray(s) {
    try {
        const v = JSON.parse(String(s));
        return Array.isArray(v) ? v.map((x) => Number(x)).filter((n) => Number.isFinite(n) && n > 0) : [];
    } catch {
        return [];
    }
}

export function safeJson(res, path) {
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

export function resolveMemberSource(memberRange, memberIds) {
    if (memberRange) return { type: "range", range: memberRange };
    if (memberIds.length > 0) return { type: "list", list: memberIds };
    return null;
}

export function pickMembersForTokenIssue(memberSource, tokenStrategy, tokenPoolSize) {
    if (memberSource.type === "list") {
        return memberSource.list;
    }

    const { min, max } = memberSource.range;
    const total = max - min + 1;

    if (tokenStrategy === "all") {
        const out = [];
        for (let i = 0; i < total; i++) out.push(min + i);
        return out;
    }

    // TOKEN_STRATEGY=pool
    const size = Math.min(tokenPoolSize, total);
    const out = [];
    for (let i = 0; i < size; i++) out.push(min + i);
    return out;
}

export function setupTokens(baseUrl, testTokenEndpoint, memberSource, tokenStrategy, tokenPoolSize) {
    if (!memberSource) {
        fail("No member source. Provide MEMBER_ID_RANGE=\"a-b\" or MEMBER_IDS=\"1,2,3\".");
    }

    const idsToIssue = pickMembersForTokenIssue(memberSource, tokenStrategy, tokenPoolSize);

    const tokenMap = {};
    for (const memberId of idsToIssue) {
        const url = `${baseUrl}${testTokenEndpoint}?memberId=${memberId}`;
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

export function pickMemberIdForVu(issuedMemberIds) {
    if (!issuedMemberIds || issuedMemberIds.length === 0) fail("No issuedMemberIds in setup data");
    const idx = (__VU - 1) % issuedMemberIds.length;
    return issuedMemberIds[idx];
}

export function pickTicketId(seatMode, ticketRange, ticketIds, hotPoolSize) {
    // 1) hot mode: range가 있으면 앞 HOT_POOL_SIZE만 집중
    if (seatMode === "hot" && ticketRange) {
        const min = ticketRange.min;
        const max = Math.min(ticketRange.max, min + hotPoolSize - 1);
        return min + Math.floor(Math.random() * (max - min + 1));
    }

    // 2) spread: range 전체
    if (ticketRange) {
        return ticketRange.min + Math.floor(Math.random() * (ticketRange.max - ticketRange.min + 1));
    }

    // 3) fallback: 배열
    if (ticketIds.length > 0) {
        if (seatMode === "hot") {
            const pool = ticketIds.slice(0, Math.min(hotPoolSize, ticketIds.length));
            return pool[Math.floor(Math.random() * pool.length)];
        }
        return ticketIds[Math.floor(Math.random() * ticketIds.length)];
    }

    return null;
}