INSERT INTO member (
    created_at,
    updated_at,
    member_email,
    is_seller,
    member_nickname,
    member_state
)
SELECT
    NOW(6),
    NOW(6),
    CONCAT('user', seq, '@test.local'),
    b'0',
    CONCAT('user', seq),
    'ACTIVE'
FROM (
         SELECT @row := @row + 1 AS seq
         FROM information_schema.columns, (SELECT @row := 0) r
         LIMIT 20000
     ) t;

INSERT INTO hall (
    created_at,
    updated_at,
    hall_name,
    hall_state,
    hall_type
)
SELECT
    NOW(6),
    NOW(6),
    CONCAT('HALL-', seq),
    'AVAILABLE',
    'LARGE'
FROM (
         SELECT @row2 := @row2 + 1 AS seq
         FROM information_schema.columns, (SELECT @row2 := 0) r
         LIMIT 25
     ) t;

INSERT INTO concert (
    created_at,
    updated_at,
    concert_age,
    concert_booking_open,
    concert_detail,
    concert_duration,
    concert_end,
    concert_name,
    concert_start,
    concert_thumbnail,
    hall_id,
    member_id
)
SELECT
    NOW(6),
    NOW(6),
    12,
    NOW(6),
    'load test concert',
    '120min',
    DATE_ADD(NOW(6), INTERVAL 1 DAY),
    CONCAT('CONCERT-', seq),
    DATE_ADD(NOW(6), INTERVAL 1 DAY),
    NULL,
    seq,        -- hall_id = 1..25
    1           -- seller
FROM (
         SELECT @row3 := @row3 + 1 AS seq
         FROM information_schema.columns, (SELECT @row3 := 0) r
         LIMIT 25
     ) t;

INSERT INTO session (
    created_at,
    updated_at,
    session_date,
    session_price,
    concert_id
)
SELECT
    NOW(6),
    NOW(6),
    DATE_ADD(NOW(6), INTERVAL 10 MINUTE),
    10000,
    concert_id
FROM concert
WHERE concert_id BETWEEN 1 AND 25;

INSERT INTO ticket (
    created_at,
    updated_at,
    expires_at,
    hold_token,
    seat_num,
    ticket_state,
    member_id,
    session_id
)
SELECT
    NOW(6),
    NOW(6),
    NULL,
    NULL,
    seat_num,
    'AVAILABLE',
    member_id,
    session_id
FROM (
         SELECT
             m.member_id,
             s.session_id,
             ((m.member_id - 1) % 800) + 1 AS seat_num
         FROM member m
                  JOIN session s
                       ON s.session_id = ((m.member_id - 1) DIV 800) + 1
         WHERE m.member_id BETWEEN 1 AND 20000
     ) t;

INSERT INTO session_start_alarm (
    created_at,
    updated_at,
    session_start_alarm_at,
    session_start_alarm_state,
    session_start_alarm_type,
    member_id,
    session_id
)
SELECT
    NOW(6),
    NOW(6),
    DATE_ADD(NOW(6), INTERVAL 10 MINUTE),
    'PENDING',
    'LEFT_10',
    member_id,
    session_id
FROM (
         SELECT
             m.member_id,
             s.session_id
         FROM member m
                  JOIN session s
                       ON s.session_id = ((m.member_id - 1) DIV 800) + 1
         WHERE m.member_id BETWEEN 1 AND 20000
     ) t;
