package stack.moaticket.domain.faq_question.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDTO;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqQuestionService {
    private final FaqQuestionRepository faqQuestionRepository;

    public final class AuthValidator {

        private AuthValidator() {}

        public static void checkAuthenticated(Member member) {
            if (member == null || member.getId() == null) {
                throw new MoaException(MoaExceptionType.FORBIDDEN);
            }
        }
    }

    public static <T extends Ownable> void checkOwner(T data, Member member) {
        if(member == null || member.getId() == null) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        if(data.getMember() == null || !data.getMember().getId().equals(member.getId())) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }

    // 글 생성
    @Transactional
    public FaqQuestionResponseDTO createQuestion(Member member, FaqQuestionRequestDTO rqdto, MultipartFile file) {

        // 중복 체크
        if(faqQuestionRepository.existsByTitle((rqdto.getTitle()))) {
            throw new MoaException(MoaExceptionType.ALREADY_QUESTION);
        }

        // 엔티티 생성
        FaqQuestion faqQuestion = FaqQuestion.builder().title(rqdto.getTitle()).contents(rqdto.getContent())
                .faqType(rqdto.getOption()).member(member).build();

        checkOwner(faqQuestion, member);

        // 파일 처리
        if(file != null && !file.isEmpty()) {
            // 파일 처리 비즈니스 로직
        }

        // 저장
        FaqQuestion savedQuestionData = faqQuestionRepository.save(faqQuestion);

        return FaqQuestionResponseDTO.fromEntity(savedQuestionData);
    }

    // 글 조회
    @Transactional(readOnly = true)
    public Page<FaqQuestionResponseDTO> readQuestionList(Member member, int pageNo, String criteria) {
        int PAGE_SIZE = 10;
        AuthValidator.checkAuthenticated(member);
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, criteria));
        Page<FaqQuestionResponseDTO> page = faqQuestionRepository.findAll(pageable).map(FaqQuestionResponseDTO::fromEntity);
        return page;
    }

    // 글 수정
    @Transactional
    public FaqQuestionResponseDTO updateQuestion(Member member, Long id, FaqQuestionRequestDTO rqdto, MultipartFile File) {

        // 기존의 엔티티 조회
        FaqQuestion faqQuestionById = faqQuestionRepository.findById(id).orElseThrow(() -> {
            return new MoaException(MoaExceptionType.NOT_FOUND);
        });

        checkOwner(faqQuestionById, member);

        // 값 변경하기(null이면 변경하지 않는다.)
        if(rqdto.getTitle() != null) {
            faqQuestionById.setTitle(rqdto.getTitle());
        }

        if(rqdto.getContent() != null) {
            faqQuestionById.setContents(rqdto.getContent());
        }

        if(rqdto.getOption() != null) {
            faqQuestionById.setFaqType(rqdto.getOption());
        }


        return FaqQuestionResponseDTO.fromEntity(faqQuestionById);
    }

    // 글 삭제
    public FaqQuestionResponseDTO deleteQuestion(Member member, Long id) {

        FaqQuestion deletedQuestion = FaqQuestion.builder().id(id).build();

        checkOwner(deletedQuestion, member);

        faqQuestionRepository.deleteById(id);

        return FaqQuestionResponseDTO.fromEntity(deletedQuestion);
    }
}
