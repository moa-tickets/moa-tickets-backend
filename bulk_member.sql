-- ============================================
-- Load test members bulk insert
-- member_id는 AUTO_INCREMENT 이므로 제외
-- ============================================

# show session variables like '%cte_max_recursion_depth';
# show global variables like '%cte_max_recursion_depth';
# set session cte_max_recursion_depth=30000;
# set session cte_max_recursion_depth=1000;

use moa;
SELECT VERSION();

INSERT INTO member (
    member_nickname,
    member_state,
    is_seller,
    member_email,
    created_at,
    updated_at
)
SELECT
    CONCAT('loadtest_user_', LPAD(n, 6, '0')) AS member_nickname,
    'ACTIVE' AS member_state,
    FALSE AS is_seller,
    CONCAT('loadtest_', LPAD(n, 6, '0'), '@moa.local') AS member_email,
    NOW(),
    NOW()
FROM (
         SELECT
             (a.n
                 + b.n * 10
                 + c.n * 100
                 + d.n * 1000
                 + e.n * 10000) + 1 AS n
         FROM
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
                 CROSS JOIN
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
                 CROSS JOIN
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
                 CROSS JOIN
             (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
                 CROSS JOIN
             (SELECT 0 n UNION ALL SELECT 1) e
     ) numbers
WHERE n <= 20000;

# select * from member where member_email like 'loadtest_000001@moa.local';
#
# select * from member;
