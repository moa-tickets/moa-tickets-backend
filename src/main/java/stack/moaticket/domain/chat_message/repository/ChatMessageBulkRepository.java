package stack.moaticket.domain.chat_message.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.chat_message.entity.ChatMessage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageBulkRepository {
    private final JdbcTemplate jdbcTemplate;

    public void saveAllMessages(List<ChatMessage> messages) {
        String sql = "INSERT INTO chat_message " +
                "(content, nickname, chatroom_id, timestamp, member_id, created_at, updated_at)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ChatMessage chatMessage = messages.get(i);
                ps.setString(1, chatMessage.getContent());
                ps.setString(2, chatMessage.getNickname());
                ps.setString(3, chatMessage.getChatroomId());
                ps.setTimestamp(4, Timestamp.valueOf(chatMessage.getTimestamp()));
                ps.setLong(5, chatMessage.getMemberId());
                ps.setTimestamp(6, Timestamp.valueOf(now));
                ps.setTimestamp(7, Timestamp.valueOf(now));
            }

            @Override
            public int getBatchSize() {
                return messages.size();
            }
        });
    }
}
