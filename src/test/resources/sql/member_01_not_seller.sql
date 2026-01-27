DELETE FROM member where member_id = 1;

INSERT INTO member (member_id, member_nickname, member_state, member_email, is_seller, created_at, updated_at)
VALUES (1, 'test', 'ACTIVE', 'test@moa.dev', false, now(), now());