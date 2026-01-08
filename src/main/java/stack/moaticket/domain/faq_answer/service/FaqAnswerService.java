package stack.moaticket.domain.faq_answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.FaqAnswerDto;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.faq_answer.repository.FaqAnswerRepository;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FaqAnswerService {

    private final FaqAnswerRepository faqAnswerRepository;
    private final FaqQuestionRepository faqQuestionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FaqAnswerDto.Response createAnswer(FaqAnswerDto.CreateRequest request, Long memberId) {
        validateCreateRequest(request);

        FaqQuestion question = faqQuestionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_QUESTION_NOT_FOUND));

        if (question.isAnswered()) {
            throw new MoaException(MoaExceptionType.FAQ_QUESTION_ALREADY_ANSWERED);
        }

        // 이미 답변이 있는지 확인
        if (faqAnswerRepository.findByQuestion(question).isPresent()) {
            throw new MoaException(MoaExceptionType.FAQ_QUESTION_ALREADY_ANSWERED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        FaqAnswer answer = new FaqAnswer(question, member, request.getContent());
        FaqAnswer saved = faqAnswerRepository.save(answer);

        return FaqAnswerDto.Response.from(saved);
    }

    public FaqAnswerDto.Response getAnswerById(Long answerId) {
        FaqAnswer answer = faqAnswerRepository.findById(answerId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_ANSWER_NOT_FOUND));

        // 연관된 엔티티들을 로드하기 위해 LAZY 로딩 트리거
        answer.getQuestion().getId();
        answer.getMember().getId();

        return FaqAnswerDto.Response.from(answer);
    }

    @Transactional
    public FaqAnswerDto.Response updateAnswer(Long answerId, FaqAnswerDto.UpdateRequest request, Long memberId) {
        validateUpdateRequest(request);

        FaqAnswer answer = faqAnswerRepository.findById(answerId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_ANSWER_NOT_FOUND));

        validateOwnership(answer, memberId);

        answer.setContent(request.getContent());

        FaqAnswer saved = faqAnswerRepository.save(answer);
        return FaqAnswerDto.Response.from(saved);
    }

    @Transactional
    public void deleteAnswer(Long answerId, Long memberId) {
        FaqAnswer answer = faqAnswerRepository.findById(answerId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_ANSWER_NOT_FOUND));

        validateOwnership(answer, memberId);

        FaqQuestion question = answer.getQuestion();
        question.setAnswered(false); // 질문의 답변 상태를 false로 변경

        faqAnswerRepository.delete(answer);
    }

    private void validateCreateRequest(FaqAnswerDto.CreateRequest request) {
        if (request == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getQuestionId() == null || request.getQuestionId() <= 0) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateUpdateRequest(FaqAnswerDto.UpdateRequest request) {
        if (request == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateOwnership(Ownable ownable, Long memberId) {
        if (!ownable.getMember().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }
}
