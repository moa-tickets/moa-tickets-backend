-- =========================================================
-- DEV Seed (MySQL 8) - create-drop 전제 (삭제 없이 INSERT만)
-- =========================================================

SET time_zone = '+09:00';
START TRANSACTION;

-- 0) 변수 (collation 명시해서 비교/저장 시 문제 예방)
SET @target_name = CONVERT('모아모아구름콘서트' USING utf8mb4) COLLATE utf8mb4_unicode_ci;

-- 1) 멤버 2명
INSERT INTO `member`
(`member_nickname`, `member_state`, `is_seller`, `member_email`, `created_at`, `updated_at`)
VALUES
    ('buyer01',  'ACTIVE', 0, 'buyer01@example.com',  NOW(), NOW()),
    ('seller01', 'ACTIVE', 1, 'seller01@example.com', NOW(), NOW());

-- 2) FK용 ID 조회
SET @seller_id := (
    SELECT `member_id` FROM `member`
    WHERE `member_email` = 'seller01@example.com'
    LIMIT 1
);

SET @hall_id := (
    SELECT `hall_id` FROM `hall`
    WHERE `hall_name` = 'MOA HALL SMALL'
    LIMIT 1
);

-- 3) 콘서트 1개
INSERT INTO `concert` (
    `member_id`, `hall_id`,
    `concert_name`, `concert_duration`, `concert_age`,
    `concert_booking_open`, `concert_start`, `concert_end`,
    `concert_thumbnail`,
    `created_at`, `updated_at`
)
VALUES (
           @seller_id, @hall_id,
           @target_name, '120분', 12,
           '2026-01-01 20:00:00',
           '2026-01-03 18:00:00',
           '2026-01-04 18:00:00',
           'https://example.com/moa-cloud-thumb.jpg',
           NOW(), NOW()
       );

SET @concert_id := LAST_INSERT_ID();

-- 4) 세션 2개
INSERT INTO `session`
(`concert_id`, `session_date`, `session_price`, `created_at`, `updated_at`)
VALUES
    (@concert_id, '2026-01-03 18:00:00', 1000, NOW(), NOW()),
    (@concert_id, '2026-01-04 18:00:00', 1000, NOW(), NOW());

-- 5) 티켓 생성: 각 세션마다 seat_num 1~80
INSERT INTO `ticket` (`session_id`, `seat_num`, `ticket_state`, `created_at`, `updated_at`)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 80
)
SELECT
    s.`session_id`,
    seq.n,
    'AVAILABLE',
    NOW(),
    NOW()
FROM `session` s
         JOIN seq
WHERE s.`concert_id` = @concert_id;

COMMIT;
