WITH
    m AS (
        SELECT member_id,
               ROW_NUMBER() OVER (ORDER BY member_id) AS rn
        FROM member
        ORDER BY member_id
        LIMIT 500
    ),
    t AS (
        SELECT ticket_id,
               ROW_NUMBER() OVER (ORDER BY ticket_id) AS rn
        FROM ticket
        ORDER BY ticket_id
        LIMIT 500
    )
UPDATE ticket tk
    JOIN t ON t.ticket_id = tk.ticket_id
    JOIN m ON m.rn = ((t.rn - 1) % 500) + 1
SET
    tk.ticket_state = 'HOLD',
    tk.hold_token   = CONCAT('HT_', LPAD(tk.ticket_id, 12, '0')),
    tk.member_id    = m.member_id,
    tk.expires_at   = DATE_ADD(NOW(6), INTERVAL 3 MINUTE),
    tk.updated_at   = NOW(6);