package stack.moaticket.domain.faq_answer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.faq_answer.type.AnswerState;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.member.entity.Member;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "faq_answer")
public class FaqAnswer extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "faq_id")
    private FaqQuestion faqQuestion;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "answer_title")
    private String title;

    @Column(name = "answer_content")
    private String content;

    @Column(name = "answer_file_url")
    private String fileURL;

    @Column(name = "answer_statement")
    private AnswerState state;
}
