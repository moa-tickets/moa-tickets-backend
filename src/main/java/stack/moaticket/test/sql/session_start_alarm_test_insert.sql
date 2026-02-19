SET @MEMBERS := 1000000;
SET @SEATS_PER_SESSION := 800;
SET @SESSIONS := CEIL(@MEMBERS / @SEATS_PER_SESSION); -- 1250

SET @MEMBER_PREFIX := 'lt_user_';
SET @HALL_PREFIX := 'LT_HALL-';
SET @CONCERT_PREFIX := 'LT_CONCERT-';

SET @SELLER_ID := 1;

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
         SELECT
             (a.d
                 + b.d * 10
                 + c.d * 100
                 + d.d * 1000
                 + e.d * 10000
                 + f.d * 100000
                 ) + 1 AS n
         FROM
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) f
     ) x
WHERE x.n <= @MEMBERS;

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
         SELECT
             (a.d + b.d * 10 + c.d * 100 + d.d * 1000) + 1 AS n
         FROM
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
     ) x
WHERE x.n <= @SESSIONS;

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
    CONCAT(@CONCERT_PREFIX, x.n) COLLATE utf8mb4_unicode_ci,
    DATE_ADD(NOW(6), INTERVAL 1 DAY),
    NULL,
    h.hall_id,
    @SELLER_ID
FROM (
         SELECT
             (a.d + b.d * 10 + c.d * 100 + d.d * 1000) + 1 AS n
         FROM
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
                 CROSS JOIN
             (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
     ) x
         JOIN hall h
              ON h.hall_name COLLATE utf8mb4_unicode_ci
                  = CONCAT(@HALL_PREFIX, x.n) COLLATE utf8mb4_unicode_ci
WHERE x.n <= @SESSIONS;

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
LIMIT 1250;

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
         LIMIT 1000000
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
             LIMIT 1250
         ) ss
) s
              ON s.rn = FLOOR((m.rn - 1) / @SEATS_PER_SESSION) + 1;

INSERT INTO session_start_alarm (
    created_at, updated_at,
    session_start_alarm_at,
    session_start_alarm_state,
    session_start_alarm_type,
    member_id, session_id
)
SELECT
    NOW(6), NOW(6),
    DATE_ADD(
            DATE_ADD(NOW(6), INTERVAL 10 MINUTE),
            INTERVAL (m.rn % 1000) MICROSECOND
    ),
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
         LIMIT 1000000
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
             LIMIT 1250
         ) ss
) s
              ON s.rn = FLOOR((m.rn - 1) / @SEATS_PER_SESSION) + 1;
