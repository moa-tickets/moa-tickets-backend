set foreign_key_checks = 0;

truncate session_stream;
truncate payment_ticket;
truncate oauth_info;
truncate session_start_alarm;
truncate chat_message;
truncate concert_review;
truncate ticket_alarm;
truncate faq_answer;
truncate faq_question;
truncate payment;
truncate ticket;
truncate session;
truncate concert;
truncate review;
truncate hall;
truncate member;

set foreign_key_checks = 1;