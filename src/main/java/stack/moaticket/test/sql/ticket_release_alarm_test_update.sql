UPDATE ticket tk
    JOIN (
        SELECT
            ticket_id,
            ROW_NUMBER() OVER (ORDER BY ticket_id) AS rn
        FROM ticket
        ORDER BY ticket_id
        LIMIT 100000
    ) t ON t.ticket_id = tk.ticket_id
SET
    tk.ticket_state = 'HOLD',
    tk.hold_token   = CONCAT('HT_', LPAD(tk.ticket_id, 12, '0')),
    tk.member_id    = 1 + MOD((t.rn - 1), 1000000),
    tk.expires_at   = DATE_ADD(NOW(6), INTERVAL 3 MINUTE),
    tk.updated_at   = NOW(6);