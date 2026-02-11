UPDATE session_start_alarm
SET
    session_start_alarm_at = NOW(6),
    session_start_alarm_state = 'PENDING'
