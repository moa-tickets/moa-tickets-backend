package stack.moaticket.domain.faq_question.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "faq_question")
public class FaqQuestion extends Base implements Ownable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false)
    private boolean answered = false;

    @OneToOne(mappedBy = "question", fetch = FetchType.LAZY)
    private FaqAnswer faqAnswer;

    public void markAnswered() {
        this.answered = true;
        super.setUpdatedAt(LocalDateTime.now());
    }
}
