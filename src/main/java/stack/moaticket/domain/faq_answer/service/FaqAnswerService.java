package stack.moaticket.domain.faq_answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerRequestDTO;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerResponseDTO;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.faq_answer.repository.FaqAnswerRepository;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@Service
@RequiredArgsConstructor
public class FaqAnswerService {

    private final FaqAnswerRepository faqAnswerRepository;
    private final MemberRepository memberRepository;
    private final FaqQuestionRepository faqQuestionRepository;

    public static <T extends Ownable> void checkOwner(T data, Member member) {
        if (member == null || member.getId() == null || !member.isSeller()) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        if (data.getMember() == null || !data.getMember().getId().equals(member.getId())) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }

    @Transactional
    public FaqAnswerResponseDTO answerToQuestionPost(FaqAnswerRequestDTO dto, Member member, Long questionId) {
        // 1. 질문 조회
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        // 2. 이미 답변했는지 확인 (질문 기준)
        boolean answerExist = faqAnswerRepository.existsByMemberAndQuestion(member, question);



        // 3. 답변이 이미 작성된 경우
        if(answerExist) {
            throw new MoaException(MoaExceptionType.ALREADY_QUESTION);
        }

        // 4. 엔티티 작성
        FaqAnswer faqAnswer = FaqAnswer.builder().member(member).question(question).id(dto.getId()).content(dto.getContent()).build();

        checkOwner(faqAnswer, member);

        return FaqAnswerResponseDTO.fromEntity(faqAnswer);
    }

    @Transactional(readOnly = true)
    public FaqAnswerResponseDTO getAnswerData(Member member, Long questionId) {
        // 1. 질문 존재 여부 확인
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        // 2. 답변 조회
        FaqAnswer answer = faqAnswerRepository.findByQuestion(question)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        checkOwner(answer,member);

        // 3. DTO 변환 후 반환
        return FaqAnswerResponseDTO.fromEntity(answer);
    }

    @Transactional
    public FaqAnswerResponseDTO updateAnswerData(FaqAnswerRequestDTO dto, Member member, Long questionId) {

        // 1. 질문 존재 여부 확인
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        // 2. 기존 답변 찾기
        FaqAnswer answer = faqAnswerRepository.findByQuestion(question)
                .orElseThrow(() ->new MoaException(MoaExceptionType.NOT_FOUND));

        // 3. 답변에서 수정하기
        FaqAnswer updatedAnswer = FaqAnswer.builder()
                .id(dto.getId()).title(dto.getTitle()).content(dto.getContent()).build();

        checkOwner(updatedAnswer, member);

        // 변환
        return FaqAnswerResponseDTO.fromEntity(updatedAnswer);
    }

    @Transactional
    public FaqAnswerResponseDTO deleteAnswerData(Member member, Long questionId) {

        // 1. 질문 조회
        FaqQuestion question = faqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        // 2. 답변 조회
        FaqAnswer answer = faqAnswerRepository.findByQuestion(question)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        // 3. 권한 체크 (작성자 or 판매자)
        checkOwner(answer, member);

        // 4. 삭제
        faqAnswerRepository.delete(answer);

        // 5. 삭제된 데이터 반환 (선택)
        return FaqAnswerResponseDTO.fromEntity(answer);
    }
}
