package stack.moaticket.domain.chat_message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "chat_message",
    indexes = {
        @Index(name= "idx_message_room_timestamp", columnList = "chatroom_id, timestamp")
    }
)
public class ChatMessage extends Base{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @Column(name = "chatroom_id", nullable = false, updatable = false)
    private String chatroomId;

    @Column(name = "nickname", nullable = false, updatable = false)
    private String nickname;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "content", nullable = false, updatable = false)
    private String content;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = true)
    private LocalDateTime updatedAt;
}
