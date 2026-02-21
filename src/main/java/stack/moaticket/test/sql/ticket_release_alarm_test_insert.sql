INSERT IGNORE INTO ticket_alarm (created_at, updated_at, member_id, ticket_id)
SELECT
    NOW(6) AS created_at,
    NOW(6) AS updated_at,
    m.member_id,
    t.ticket_id
FROM
    (SELECT member_id FROM member ORDER BY member_id LIMIT 500) m
        CROSS JOIN
    (SELECT ticket_id FROM ticket ORDER BY ticket_id LIMIT 500) t;