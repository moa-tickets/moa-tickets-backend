-- =========================================
-- LoadTest Seed Script (NO TMP)
-- IntelliJ DB Console + MySQL 8.0
-- Collation mix fix included
-- LIMIT에 user variable 사용 금지(파서 이슈 회피)
-- =========================================

SET SESSION cte_max_recursion_depth = 50000;

-- ====== 여기만 바꿔서 규모 조절 ======
SET @MEMBERS := 20000;
SET @SEATS_PER_SESSION := 800;
-- @SESSIONS = CEIL(20000/800) = 25 (아래는 숫자로 박음)
-- @HALLS = 25 (아래는 숫자로 박음)

-- Prefix (실행 데이터 구분)
SET @MEMBER_PREFIX := 'lt_user_';
SET @HALL_PREFIX := 'LT_HALL-';
SET @CONCERT_PREFIX := 'LT_CONCERT-';

SET @SELLER_ID := 1;

-- =========================================
-- 1) Member 20000
-- =========================================
INSERT INTO member (
    created_at, updated_at,
    member_email, is_seller,
    member_nickname, member_state
)
SELECT
    NOW(6), NOW(6),
    CONCAT(@MEMBER_PREFIX, n, '@test.local') COLLATE utf8mb4_unicode_ci,
    b'0',
    CONCAT(@MEMBER_PREFIX, n) COLLATE utf8mb4_unicode_ci,
    'ACTIVE'
FROM (
         WITH RECURSIVE seq AS (
             SELECT 1 AS n
             UNION ALL
             SELECT n + 1 FROM seq WHERE n < (SELECT @MEMBERS)
         )
         SELECT n FROM seq
     ) x;

-- =========================================
-- 2) Hall 25
-- =========================================
INSERT INTO hall (
    created_at, updated_at,
    hall_name, hall_state, hall_type
)
SELECT
    NOW(6), NOW(6),
    CONCAT(@HALL_PREFIX, n) COLLATE utf8mb4_unicode_ci,
    'AVAILABLE',
    'LARGE'
FROM (
         WITH RECURSIVE seq AS (
             SELECT 1 AS n
             UNION ALL
             SELECT n + 1 FROM seq WHERE n < 25
         )
         SELECT n FROM seq
     ) x;

-- =========================================
-- 3) Concert 25 (hall_name으로 매핑)
-- =========================================
INSERT INTO concert (
    created_at, updated_at,
    concert_age, concert_booking_open,
    concert_detail, concert_duration,
    concert_end, concert_name,
    concert_start, concert_thumbnail,
    hall_id, member_id
)
SELECT
    NOW(6), NOW(6),
    12,
    NOW(6),
    'load test concert',
    '120min',
    DATE_ADD(NOW(6), INTERVAL 1 DAY),
    CONCAT(@CONCERT_PREFIX, n) COLLATE utf8mb4_unicode_ci,
    DATE_ADD(NOW(6), INTERVAL 1 DAY),
    NULL,
    h.hall_id,
    @SELLER_ID
FROM (
         WITH RECURSIVE seq AS (
             SELECT 1 AS n
             UNION ALL
             SELECT n + 1 FROM seq WHERE n < 25
         )
         SELECT n FROM seq
     ) x
         JOIN hall h
              ON h.hall_name COLLATE utf8mb4_unicode_ci
                  = CONCAT(@HALL_PREFIX, x.n) COLLATE utf8mb4_unicode_ci;

-- =========================================
-- 4) Session 25 (이번 실행 concert만 대상으로)
--    LIMIT 25 고정
-- =========================================
INSERT INTO session (
    created_at, updated_at,
    session_date, session_price,
    concert_id
)
SELECT
    NOW(6), NOW(6),
    DATE_ADD(NOW(6), INTERVAL 10 MINUTE),
    10000,
    c.concert_id
FROM concert c
WHERE c.concert_name COLLATE utf8mb4_unicode_ci
          LIKE CONCAT(@CONCERT_PREFIX, '%') COLLATE utf8mb4_unicode_ci
ORDER BY c.concert_id DESC
LIMIT 25;

-- =========================================
-- 5) Ticket 20000
--    - member 20000명: LIMIT은 @MEMBERS 대신 (SELECT @MEMBERS)로 파생
--    - session 25개: LIMIT 25 고정
-- =========================================
INSERT INTO ticket (
    created_at, updated_at,
    expires_at, hold_token,
    seat_num, ticket_state,
    member_id, session_id
)
SELECT
    NOW(6), NOW(6),
    NULL, NULL,
    ((m.rn - 1) % @SEATS_PER_SESSION) + 1,
    'AVAILABLE',
    m.member_id,
    s.session_id
FROM (
         SELECT
             member_id,
             ROW_NUMBER() OVER (ORDER BY member_id) AS rn
         FROM member
         WHERE member_email COLLATE utf8mb4_unicode_ci
                   LIKE CONCAT(@MEMBER_PREFIX, '%@test.local') COLLATE utf8mb4_unicode_ci
         ORDER BY member_id
         LIMIT 20000
     ) m
         JOIN (
    SELECT
        session_id,
        ROW_NUMBER() OVER (ORDER BY session_id) AS rn
    FROM (
             SELECT s.session_id
             FROM session s
                      JOIN concert c ON c.concert_id = s.concert_id
             WHERE c.concert_name COLLATE utf8mb4_unicode_ci
                       LIKE CONCAT(@CONCERT_PREFIX, '%') COLLATE utf8mb4_unicode_ci
             ORDER BY s.session_id
             LIMIT 25
         ) ss
) s
              ON s.rn = FLOOR((m.rn - 1) / @SEATS_PER_SESSION) + 1;

-- =========================================
-- 6) Session Start Alarm 20000
-- =========================================
INSERT INTO session_start_alarm (
    created_at, updated_at,
    session_start_alarm_at,
    session_start_alarm_state,
    session_start_alarm_type,
    member_id, session_id
)
SELECT
    NOW(6), NOW(6),
    DATE_ADD(NOW(6), INTERVAL 10 MINUTE),
    'PENDING',
    'LEFT_10',
    m.member_id,
    s.session_id
FROM (
         SELECT
             member_id,
             ROW_NUMBER() OVER (ORDER BY member_id) AS rn
         FROM member
         WHERE member_email COLLATE utf8mb4_unicode_ci
                   LIKE CONCAT(@MEMBER_PREFIX, '%@test.local') COLLATE utf8mb4_unicode_ci
         ORDER BY member_id
         LIMIT 20000
     ) m
         JOIN (
    SELECT
        session_id,
        ROW_NUMBER() OVER (ORDER BY session_id) AS rn
    FROM (
             SELECT s.session_id
             FROM session s
                      JOIN concert c ON c.concert_id = s.concert_id
             WHERE c.concert_name COLLATE utf8mb4_unicode_ci
                       LIKE CONCAT(@CONCERT_PREFIX, '%') COLLATE utf8mb4_unicode_ci
             ORDER BY s.session_id
             LIMIT 25
         ) ss
) s
              ON s.rn = FLOOR((m.rn - 1) / @SEATS_PER_SESSION) + 1;