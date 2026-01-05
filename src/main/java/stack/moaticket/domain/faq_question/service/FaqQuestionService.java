package stack.moaticket.domain.faq_question.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDTO;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqQuestionService {
    private final FaqQuestionRepository faqQuestionRepository;

    public static void checkAuth(Member member) {
        // 인가 기능
        if(member.getId() == null) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }

    // 글 생성
    @Transactional
    public FaqQuestionResponseDTO createQuestion(Member member, FaqQuestionRequestDTO rqdto, MultipartFile file) {

        checkAuth(member);

        // 중복 체크
        if(faqQuestionRepository.existsByTitle((rqdto.getTitle()))) {
            throw new MoaException(MoaExceptionType.ALREADY_QUESTION);
        }

        // 엔티티 생성
        FaqQuestion faqQuestion = FaqQuestion.builder().title(rqdto.getTitle()).contents(rqdto.getContent())
                .faqType(rqdto.getOption()).member(member).build();

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
    public Page<FaqQuestionResponseDTO> readQuestionList(Member member, Pageable pageable) {
        checkAuth(member);
        Page<FaqQuestion> optFaqQuestionList = faqQuestionRepository.findAll(pageable);
        Page<FaqQuestionResponseDTO> convertFaqQuestionList = optFaqQuestionList.map(FaqQuestionResponseDTO::fromEntity);
        return convertFaqQuestionList;
    }

    // 글 수정
    @Transactional
    public FaqQuestionResponseDTO updateQuestion(Member member, Long id, FaqQuestionRequestDTO rqdto, MultipartFile File) {

        checkAuth(member);

        // 기존의 엔티티 조회
        FaqQuestion faqQuestionById = faqQuestionRepository.findById(id).orElseThrow(() -> {
            return new MoaException(MoaExceptionType.NOT_FOUND);
        });

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
}
