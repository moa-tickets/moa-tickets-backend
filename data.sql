-- =========================================================
-- DEV Seed (PostgreSQL)
-- =========================================================

SET TIME ZONE 'Asia/Seoul';

BEGIN;

-- =========================================================
-- 1) 멤버 2명 생성
-- =========================================================
INSERT INTO member (member_nickname, member_state, is_seller, member_email, created_at, updated_at)
VALUES
    ('buyer01',  'ACTIVE', FALSE, 'buyer01@example.com',  now(), now()),
    ('seller01', 'ACTIVE', TRUE,  'seller01@example.com', now(), now());

-- =========================================================
-- 2) 기존 데이터 정리 (동일 concert_name 기준)
--    - FK 때문에 ticket -> session -> concert 순서로 삭제
-- =========================================================
WITH target_concert AS (
    SELECT concert_id
    FROM concert
    WHERE concert_name = '모아구름콘서트'
),
     target_sessions AS (
         SELECT session_id
         FROM session
         WHERE concert_id IN (SELECT concert_id FROM target_concert)
     ),
     del_ticket AS (
DELETE FROM ticket
WHERE session_id IN (SELECT session_id FROM target_sessions)
    RETURNING 1
     ),
     del_session AS (
DELETE FROM session
WHERE concert_id IN (SELECT concert_id FROM target_concert)
    RETURNING 1
    )
DELETE FROM concert
WHERE concert_id IN (SELECT concert_id FROM target_concert);

-- =========================================================
-- 3) 콘서트 생성 (seller 이메일, hall_name으로 FK 조회 후 INSERT)
-- =========================================================
WITH seller AS (
    SELECT member_id
    FROM member
    WHERE member_email = 'seller01@example.com'
    LIMIT 1
    ),
    target_hall AS (
SELECT hall_id
FROM hall
WHERE hall_name = 'MOA HALL SMALL'
    LIMIT 1
    ),
    ins_concert AS (
INSERT INTO concert (
    member_id, hall_id,
    concert_name, concert_duration, concert_age,
    concert_booking_open, concert_start, concert_end,
    concert_thumbnail,
    created_at, updated_at
)
SELECT
    seller.member_id,
    target_hall.hall_id,
    '모아구름콘서트', '120분', 12,
    '2026-01-01 20:00:00'::timestamp,
    '2026-01-03 18:00:00'::timestamp,
    '2026-01-04 18:00:00'::timestamp,
    'https://example.com/moa-cloud-thumb.jpg',
    now(), now()
FROM seller
    CROSS JOIN target_hall
    RETURNING concert_id
    ),

-- =========================================================
-- 4) 세션 2개 생성 + session_id 확보
-- =========================================================
    ins_session AS (
INSERT INTO session (
    concert_id, session_date, session_price,
    created_at, updated_at
)
SELECT
    ins_concert.concert_id,
    v.session_date,
    v.session_price,
    now(), now()
FROM ins_concert
    CROSS JOIN (VALUES
    ('2026-01-03 18:00:00'::timestamp, 1000),
    ('2026-01-04 18:00:00'::timestamp, 1000)
    ) AS v(session_date, session_price)
    RETURNING session_id
    )

-- =========================================================
-- 5) 티켓 생성: 각 session_id마다 seat_num 1~80
-- =========================================================
INSERT INTO ticket (
    session_id, seat_num, ticket_state, created_at, updated_at
)
SELECT
    ins_session.session_id,
    gs AS seat_num,
    'AVAILABLE' AS ticket_state,
    now(), now()
FROM ins_session
         CROSS JOIN generate_series(1, 80) AS gs;

COMMIT;
