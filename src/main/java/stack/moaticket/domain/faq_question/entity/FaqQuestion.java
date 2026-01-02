package stack.moaticket.domain.faq_question.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "faq_question")
public class FaqQuestion extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "faq_title")
    private String title;

    @Column(name = "faq_contents", columnDefinition = "TEXT")
    private String contents;

    @Column(name = "faq_file")
    private String fileURL;

    @Column(name = "faq_type")
    private String faqType;

    @Column(name = "faq_statement")
    private boolean faqStatement;
}
