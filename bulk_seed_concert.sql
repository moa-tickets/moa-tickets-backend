-- =========================================================
-- Bulk Mock Data Seed (MySQL 8) - NO explicit concert_id/session_id
-- 로컬용이기 때문에 hall id가 1,2,3으로 설정되어있습니다. 배포에서 사용할 경우 hall id를 변경하거나 1,4,3으로 설정해야합니다.
-- =========================================================

SET time_zone = '+09:00';
START TRANSACTION;

-- 0) seller_id 확보 (없으면 FK 에러)
SET @seller_id := (
    SELECT `member_id`
    FROM `member`
    WHERE `member_email` = 'seller01@example.com'
    LIMIT 1
);

-- 1) 이번 seed 대상 콘서트 이름 목록(삭제/티켓 생성 범위)
DROP TEMPORARY TABLE IF EXISTS tmp_seed_concert_names;
CREATE TEMPORARY TABLE tmp_seed_concert_names (
                                                  concert_name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY
);

INSERT INTO tmp_seed_concert_names (concert_name) VALUES
                                                      ('모아구름콘서트'),
                                                      ('YB REMASTERED 3.0 : Transcendent - 대전'),
                                                      ('YB REMASTERED 3.0 : Transcendent - 서울'),
                                                      ('임영웅 IM HERO TOUR 2025 - 서울(고척스카이돔)'),
                                                      ('2025 다이나믹 듀오 단독 콘서트 "가끔씩 오래 보자" - 서울'),
                                                      ('볼빨간사춘기 단독 콘서트 "Red Diary" - 부산'),
                                                      ('아이유 2026 전국투어 콘서트 "The Golden Hour" - 인천'),
                                                      ('싸이 흠뻑쇼 2026 SUMMER SWAG - 서울'),
                                                      ('폴킴 "SOUL MATE" 단독 콘서트 - 대구'),
                                                      ('장범준 콘서트 "Your Jukebox" - 광주'),
                                                      ('세븐틴 SEVENTEEN TOUR "FOLLOW" - 서울(고척스카이돔)'),
                                                      ('10CM 단독 콘서트 "Spring Love" - 제주'),
                                                      ('백예린 "Every letter I sent you." 콘서트 - 서울'),
                                                      ('뉴진스 NewJeans 2026 WORLD TOUR - 서울(잠실실내체육관)'),
                                                      ('이무진 "Bloom" 단독 콘서트 - 수원'),
                                                      ('데이식스 DAY6 3RD WORLD TOUR - 대전'),
                                                      ('에픽하이 EPIK HIGH "PUMP" TOUR 2026 - 부산'),
                                                      ('멜로망스 "Tale" 전국투어 콘서트 - 전주'),
                                                      ('잔나비 JANNABI "LEGEND" TOUR - 서울'),
                                                      ('르세라핌 LE SSERAFIM 2026 ASIA TOUR - 인천(인스파이어 아레나)'),
                                                      ('스탠딩 에그 Standing Egg "Little Star" - 창원');

-- 2) 기존 seed 데이터 삭제 (ticket -> session -> concert)
DELETE t
FROM `ticket` t
         JOIN `session` s ON s.`session_id` = t.`session_id`
         JOIN `concert` c ON c.`concert_id` = s.`concert_id`
         JOIN tmp_seed_concert_names n ON n.concert_name = c.`concert_name`;

DELETE s
FROM `session` s
         JOIN `concert` c ON c.`concert_id` = s.`concert_id`
         JOIN tmp_seed_concert_names n ON n.concert_name = c.`concert_name`;

DELETE c
FROM `concert` c
         JOIN tmp_seed_concert_names n ON n.concert_name = c.`concert_name`;

-- 3) Concert bulk insert (ID 자동 생성)
INSERT INTO `concert` (
    `created_at`, `updated_at`, `concert_age`,
    `concert_booking_open`, `concert_duration`, `concert_end`,
    `concert_name`, `concert_start`, `concert_thumbnail`,
    `hall_id`, `member_id`, `concert_detail`
)
SELECT
    v.created_at, v.updated_at, v.concert_age,
    v.concert_booking_open, v.concert_duration, v.concert_end,
    v.concert_name, v.concert_start, v.concert_thumbnail,
    v.hall_id, @seller_id, v.concert_detail
FROM (
         SELECT
             '2026-01-07 13:18:32' AS created_at,
             '2026-01-07 13:18:32' AS updated_at,
             12 AS concert_age,
             '2025-11-08 11:00:00' AS concert_booking_open,
             '120분' AS concert_duration,
             '2026-05-02 18:00:00' AS concert_end,
             '모아구름콘서트' AS concert_name,
             '2026-05-01 18:00:00' AS concert_start,
             'https://ticketimage.interpark.com/Play/image/large/25/25017416_p.gif' AS concert_thumbnail,
             1 AS hall_id,
             'https://ticketimage.interpark.com/Play/image/etc/25/25017416-01.jpg' AS concert_detail

         UNION ALL SELECT
                       '2026-01-08 01:08:06','2026-01-08 01:08:06',15,'2025-11-20 14:00:00','150분','2026-02-15 19:00:00',
                       'YB REMASTERED 3.0 : Transcendent - 대전','2026-02-15 19:00:00',
                       'https://tickets.interpark.com/play/performance/yb-remastered-3-0-gK9kO6RhPIaNPHMw',
                       2,
                       'https://tickets.interpark.com/play/performance/yb-remastered-3-0-gK9kO6RhPIaNPHMw'

         UNION ALL SELECT
                       '2026-01-08 01:08:31','2026-01-08 01:08:31',15,'2025-11-20 14:00:00','150분','2026-03-09 19:00:00',
                       'YB REMASTERED 3.0 : Transcendent - 서울','2026-03-08 19:00:00',
                       'https://example.com/yb-seoul-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:08:54','2026-01-08 01:08:54',8,'2025-10-15 20:00:00','180분','2026-01-05 18:00:00',
                       '임영웅 IM HERO TOUR 2025 - 서울(고척스카이돔)','2026-01-05 18:00:00',
                       'https://tickets.interpark.com/play/performance/%EC%9E%84%EC%98%81%EC%9B%85-im-hero-tour-gKmUnKM2oJWBgLHY',
                       2,
                       'https://tickets.interpark.com/play/performance/%EC%9E%84%EC%98%81%EC%9B%85-im-hero-tour-gKmUnKM2oJWBgLHY'

         UNION ALL SELECT
                       '2026-01-08 01:09:02','2026-01-08 01:09:02',15,'2025-08-10 14:00:00','120분','2025-12-21 19:30:00',
                       '2025 다이나믹 듀오 단독 콘서트 "가끔씩 오래 보자" - 서울','2025-12-20 19:30:00',
                       'https://example.com/dd-seoul-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:10','2026-01-08 01:09:10',12,'2026-01-10 14:00:00','110분','2026-03-22 19:00:00',
                       '볼빨간사춘기 단독 콘서트 "Red Diary" - 부산','2026-03-22 19:00:00',
                       'https://example.com/bol4-busan-thumb.jpg',
                       1,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:17','2026-01-08 01:09:17',8,'2025-12-01 20:00:00','150분','2026-04-13 18:00:00',
                       '아이유 2026 전국투어 콘서트 "The Golden Hour" - 인천','2026-04-12 18:00:00',
                       'https://example.com/iu-incheon-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:24','2026-01-08 01:09:24',15,'2026-03-01 14:00:00','180분','2026-07-19 19:00:00',
                       '싸이 흠뻑쇼 2026 SUMMER SWAG - 서울','2026-07-18 19:00:00',
                       'https://example.com/psy-seoul-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:33','2026-01-08 01:09:33',10,'2025-07-20 14:00:00','120분','2025-11-30 19:00:00',
                       '폴킴 "SOUL MATE" 단독 콘서트 - 대구','2025-11-30 19:00:00',
                       'https://example.com/paulkim-daegu-thumb.jpg',
                       1,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:40','2026-01-08 01:09:40',12,'2026-01-15 14:00:00','130분','2026-05-10 19:00:00',
                       '장범준 콘서트 "Your Jukebox" - 광주','2026-05-10 19:00:00',
                       'https://example.com/jang-gwangju-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:09:54','2026-01-08 01:09:54',8,'2025-09-10 20:00:00','180분','2026-01-12 18:00:00',
                       '세븐틴 SEVENTEEN TOUR "FOLLOW" - 서울(고척스카이돔)','2026-01-10 18:00:00',
                       'https://example.com/seventeen-seoul-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:01','2026-01-08 01:10:01',10,'2026-02-01 14:00:00','110분','2026-04-05 19:00:00',
                       '10CM 단독 콘서트 "Spring Love" - 제주','2026-04-05 19:00:00',
                       'https://example.com/10cm-jeju-thumb.jpg',
                       1,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:08','2026-01-08 01:10:08',12,'2025-06-15 14:00:00','120분','2025-10-26 19:30:00',
                       '백예린 "Every letter I sent you." 콘서트 - 서울','2025-10-25 19:30:00',
                       'https://example.com/yerin-seoul-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:14','2026-01-08 01:10:14',8,'2026-02-20 20:00:00','150분','2026-06-15 18:00:00',
                       '뉴진스 NewJeans 2026 WORLD TOUR - 서울(잠실실내체육관)','2026-06-14 18:00:00',
                       'https://example.com/newjeans-seoul-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:20','2026-01-08 01:10:20',10,'2025-05-20 14:00:00','120분','2025-09-28 19:00:00',
                       '이무진 "Bloom" 단독 콘서트 - 수원','2025-09-28 19:00:00',
                       'https://example.com/lee-suwon-thumb.jpg',
                       1,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:28','2026-01-08 01:10:28',12,'2026-01-05 14:00:00','150분','2026-04-20 18:00:00',
                       '데이식스 DAY6 3RD WORLD TOUR - 대전','2026-04-20 18:00:00',
                       'https://example.com/day6-daejeon-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:36','2026-01-08 01:10:36',15,'2025-11-01 14:00:00','140분','2026-02-02 19:00:00',
                       '에픽하이 EPIK HIGH "PUMP" TOUR 2026 - 부산','2026-02-01 19:00:00',
                       'https://example.com/epikhigh-busan-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:42','2026-01-08 01:10:42',10,'2026-03-10 14:00:00','110분','2026-06-07 19:00:00',
                       '멜로망스 "Tale" 전국투어 콘서트 - 전주','2026-06-07 19:00:00',
                       'https://example.com/melomance-jeonju-thumb.jpg',
                       1,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:48','2026-01-08 01:10:48',12,'2025-08-25 14:00:00','130분','2025-12-15 19:00:00',
                       '잔나비 JANNABI "LEGEND" TOUR - 서울','2025-12-14 19:00:00',
                       'https://example.com/jannabi-seoul-thumb.jpg',
                       3,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:10:55','2026-01-08 01:10:55',8,'2026-02-15 20:00:00','160분','2026-05-31 18:00:00',
                       '르세라핌 LE SSERAFIM 2026 ASIA TOUR - 인천(인스파이어 아레나)','2026-05-30 18:00:00',
                       'https://example.com/lesserafim-incheon-thumb.jpg',
                       2,
                       NULL

         UNION ALL SELECT
                       '2026-01-08 01:11:02','2026-01-08 01:11:02',10,'2025-04-10 14:00:00','115분','2025-08-16 19:00:00',
                       '스탠딩 에그 Standing Egg "Little Star" - 창원','2025-08-16 19:00:00',
                       'https://example.com/standingegg-changwon-thumb.jpg',
                       1,
                       NULL
     ) v;

-- 4) Session bulk insert (concert_name으로 concert_id 매핑)
INSERT INTO `session` (`created_at`, `updated_at`, `session_date`, `session_price`, `concert_id`)
SELECT
    v.created_at, v.updated_at, v.session_date, v.session_price,
    c.`concert_id`
FROM (
         SELECT '2026-01-07 13:18:32' AS created_at, '2026-01-07 13:18:32' AS updated_at, '2026-05-01 18:00:00' AS session_date, 1000 AS session_price, '모아구름콘서트' AS concert_name
         UNION ALL SELECT '2026-01-07 13:18:32','2026-01-07 13:18:32','2026-05-02 18:00:00',2000,'모아구름콘서트'
         UNION ALL SELECT '2026-01-08 01:08:06','2026-01-08 01:08:06','2026-02-15 19:00:00',45000,'YB REMASTERED 3.0 : Transcendent - 대전'
         UNION ALL SELECT '2026-01-08 01:08:31','2026-01-08 01:08:31','2026-03-08 19:00:00',50000,'YB REMASTERED 3.0 : Transcendent - 서울'
         UNION ALL SELECT '2026-01-08 01:08:31','2026-01-08 01:08:31','2026-03-09 19:00:00',50000,'YB REMASTERED 3.0 : Transcendent - 서울'
         UNION ALL SELECT '2026-01-08 01:08:54','2026-01-08 01:08:54','2026-01-05 18:00:00',50000,'임영웅 IM HERO TOUR 2025 - 서울(고척스카이돔)'
         UNION ALL SELECT '2026-01-08 01:09:02','2026-01-08 01:09:02','2025-12-20 19:30:00',35000,'2025 다이나믹 듀오 단독 콘서트 "가끔씩 오래 보자" - 서울'
         UNION ALL SELECT '2026-01-08 01:09:02','2026-01-08 01:09:02','2025-12-21 19:30:00',35000,'2025 다이나믹 듀오 단독 콘서트 "가끔씩 오래 보자" - 서울'
         UNION ALL SELECT '2026-01-08 01:09:10','2026-01-08 01:09:10','2026-03-22 19:00:00',25000,'볼빨간사춘기 단독 콘서트 "Red Diary" - 부산'
         UNION ALL SELECT '2026-01-08 01:09:17','2026-01-08 01:09:17','2026-04-12 18:00:00',50000,'아이유 2026 전국투어 콘서트 "The Golden Hour" - 인천'
         UNION ALL SELECT '2026-01-08 01:09:17','2026-01-08 01:09:17','2026-04-13 18:00:00',50000,'아이유 2026 전국투어 콘서트 "The Golden Hour" - 인천'
         UNION ALL SELECT '2026-01-08 01:09:24','2026-01-08 01:09:24','2026-07-18 19:00:00',45000,'싸이 흠뻑쇼 2026 SUMMER SWAG - 서울'
         UNION ALL SELECT '2026-01-08 01:09:24','2026-01-08 01:09:24','2026-07-19 19:00:00',45000,'싸이 흠뻑쇼 2026 SUMMER SWAG - 서울'
         UNION ALL SELECT '2026-01-08 01:09:33','2026-01-08 01:09:33','2025-11-30 19:00:00',30000,'폴킴 "SOUL MATE" 단독 콘서트 - 대구'
         UNION ALL SELECT '2026-01-08 01:09:40','2026-01-08 01:09:40','2026-05-10 19:00:00',35000,'장범준 콘서트 "Your Jukebox" - 광주'
         UNION ALL SELECT '2026-01-08 01:09:54','2026-01-08 01:09:54','2026-01-10 18:00:00',50000,'세븐틴 SEVENTEEN TOUR "FOLLOW" - 서울(고척스카이돔)'
         UNION ALL SELECT '2026-01-08 01:09:54','2026-01-08 01:09:54','2026-01-11 18:00:00',50000,'세븐틴 SEVENTEEN TOUR "FOLLOW" - 서울(고척스카이돔)'
         UNION ALL SELECT '2026-01-08 01:09:54','2026-01-08 01:09:54','2026-01-12 18:00:00',50000,'세븐틴 SEVENTEEN TOUR "FOLLOW" - 서울(고척스카이돔)'
         UNION ALL SELECT '2026-01-08 01:10:01','2026-01-08 01:10:01','2026-04-05 19:00:00',28000,'10CM 단독 콘서트 "Spring Love" - 제주'
         UNION ALL SELECT '2026-01-08 01:10:08','2026-01-08 01:10:08','2025-10-25 19:30:00',40000,'백예린 "Every letter I sent you." 콘서트 - 서울'
         UNION ALL SELECT '2026-01-08 01:10:08','2026-01-08 01:10:08','2025-10-26 19:30:00',40000,'백예린 "Every letter I sent you." 콘서트 - 서울'
         UNION ALL SELECT '2026-01-08 01:10:14','2026-01-08 01:10:14','2026-06-14 18:00:00',50000,'뉴진스 NewJeans 2026 WORLD TOUR - 서울(잠실실내체육관)'
         UNION ALL SELECT '2026-01-08 01:10:14','2026-01-08 01:10:14','2026-06-15 18:00:00',50000,'뉴진스 NewJeans 2026 WORLD TOUR - 서울(잠실실내체육관)'
         UNION ALL SELECT '2026-01-08 01:10:20','2026-01-08 01:10:20','2025-09-28 19:00:00',30000,'이무진 "Bloom" 단독 콘서트 - 수원'
         UNION ALL SELECT '2026-01-08 01:10:28','2026-01-08 01:10:28','2026-04-20 18:00:00',42000,'데이식스 DAY6 3RD WORLD TOUR - 대전'
         UNION ALL SELECT '2026-01-08 01:10:36','2026-01-08 01:10:36','2026-02-01 19:00:00',45000,'에픽하이 EPIK HIGH "PUMP" TOUR 2026 - 부산'
         UNION ALL SELECT '2026-01-08 01:10:36','2026-01-08 01:10:36','2026-02-02 19:00:00',45000,'에픽하이 EPIK HIGH "PUMP" TOUR 2026 - 부산'
         UNION ALL SELECT '2026-01-08 01:10:42','2026-01-08 01:10:42','2026-06-07 19:00:00',27000,'멜로망스 "Tale" 전국투어 콘서트 - 전주'
         UNION ALL SELECT '2026-01-08 01:10:48','2026-01-08 01:10:48','2025-12-14 19:00:00',38000,'잔나비 JANNABI "LEGEND" TOUR - 서울'
         UNION ALL SELECT '2026-01-08 01:10:48','2026-01-08 01:10:48','2025-12-15 19:00:00',38000,'잔나비 JANNABI "LEGEND" TOUR - 서울'
         UNION ALL SELECT '2026-01-08 01:10:55','2026-01-08 01:10:55','2026-05-30 18:00:00',50000,'르세라핌 LE SSERAFIM 2026 ASIA TOUR - 인천(인스파이어 아레나)'
         UNION ALL SELECT '2026-01-08 01:10:55','2026-01-08 01:10:55','2026-05-31 18:00:00',50000,'르세라핌 LE SSERAFIM 2026 ASIA TOUR - 인천(인스파이어 아레나)'
         UNION ALL SELECT '2026-01-08 01:11:02','2026-01-08 01:11:02','2025-08-16 19:00:00',25000,'스탠딩 에그 Standing Egg "Little Star" - 창원'
     ) v
         JOIN `concert` c
              ON c.`concert_name` = v.concert_name;

-- 5) 티켓 1~80 생성 (seed 대상 콘서트들에 속한 모든 session)

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
    NOW(), NOW()
FROM `session` s
         JOIN `concert` c ON c.`concert_id` = s.`concert_id`
         JOIN tmp_seed_concert_names n ON n.concert_name = c.`concert_name`
         JOIN seq;

COMMIT;
