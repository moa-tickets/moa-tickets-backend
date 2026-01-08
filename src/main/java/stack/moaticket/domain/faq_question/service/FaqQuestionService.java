package stack.moaticket.domain.faq_question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.FaqQuestionDto;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FaqQuestionService {

    private final FaqQuestionRepository faqQuestionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FaqQuestionDto.Response createQuestion(FaqQuestionDto.CreateRequest request, Long memberId) {
        validateCreateRequest(request);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        FaqQuestion question = FaqQuestion.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .answered(false)
                .build();

        FaqQuestion saved = faqQuestionRepository.save(question);
        return FaqQuestionDto.Response.from(saved);
    }

    public Page<FaqQuestionDto.SimpleResponse> getAllQuestions(Pageable pageable) {
        Page<FaqQuestion> questions = faqQuestionRepository.findAll(pageable);
        return questions.map(FaqQuestionDto.SimpleResponse::from);
    }

    public FaqQuestionDto.Response getQuestionById(Long questionId) {
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_QUESTION_NOT_FOUND));

        // 답변을 함께 로드하기 위해 명시적으로 로드
        if (question.getFaqAnswer() != null) {
            question.getFaqAnswer().getContent(); // LAZY 로딩 트리거
        }

        return FaqQuestionDto.Response.from(question);
    }

    @Transactional
    public FaqQuestionDto.Response updateQuestion(Long questionId, FaqQuestionDto.UpdateRequest request, Long memberId) {
        validateUpdateRequest(request);

        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_QUESTION_NOT_FOUND));

        validateOwnership(question, memberId);

        question.setTitle(request.getTitle());
        question.setContent(request.getContent());

        FaqQuestion saved = faqQuestionRepository.save(question);
        return FaqQuestionDto.Response.from(saved);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long memberId) {
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.FAQ_QUESTION_NOT_FOUND));

        validateOwnership(question, memberId);

        // 답변이 있는 경우 답변도 함께 삭제해야 할 수 있음
        // 현재는 CASCADE 설정이 없으므로 수동으로 확인
        if (question.getFaqAnswer() != null) {
            throw new MoaException(MoaExceptionType.CONFLICT, "답변이 있는 질문은 삭제할 수 없습니다.");
        }

        faqQuestionRepository.delete(question);
    }

    private void validateCreateRequest(FaqQuestionDto.CreateRequest request) {
        if (request == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getTitle().length() > 100) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent().length() > 255) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateUpdateRequest(FaqQuestionDto.UpdateRequest request) {
        if (request == null) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getTitle().length() > 100) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
        if (request.getContent().length() > 255) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }
    }

    private void validateOwnership(Ownable ownable, Long memberId) {
        if (!ownable.getMember().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }
}
