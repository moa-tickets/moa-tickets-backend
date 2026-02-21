SET @JITTER_US := 1000000; -- 0 ~ 999,999us (최대 1초 분산)

UPDATE session_start_alarm a
SET
    a.session_start_alarm_at =
        DATE_ADD(
                DATE_ADD(NOW(6), INTERVAL 3 MINUTE),
                INTERVAL (CRC32(CONCAT(a.member_id, '-', a.session_id)) % @JITTER_US) MICROSECOND
        ),
    a.session_start_alarm_state = 'PENDING',
    a.updated_at = NOW(6)
WHERE a.session_start_alarm_type = 'LEFT_10';
