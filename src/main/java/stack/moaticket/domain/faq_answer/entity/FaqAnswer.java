package stack.moaticket.domain.faq_answer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.faq_answer.type.AnswerState;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Table(name = "faq_answer", uniqueConstraints =  {
        @UniqueConstraint(columnNames = "question_id")
})
public class FaqAnswer extends Base implements Ownable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FaqQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public FaqAnswer(FaqQuestion question, Member member, String content) {
        this.question = question;
        this.member = member;
        this.content = content;
        super.setCreatedAt(LocalDateTime.now());

        // 질문 상태 변경
        question.markAnswered();
    }

}
