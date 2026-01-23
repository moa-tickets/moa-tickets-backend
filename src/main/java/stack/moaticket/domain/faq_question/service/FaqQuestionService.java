package stack.moaticket.domain.faq_question.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDto;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDto;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.faq_question.entity.Ownable;
import stack.moaticket.domain.faq_question.repository.FaqQuestionRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.util.AuthValidator;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FaqQuestionService {
    private final Validator validator;

    private final MemberService memberService;

    private final FaqQuestionRepository faqQuestionRepository;
    private static final int PAGE_SIZE = 10;

    public static <T extends Ownable> void checkOwner(T data, Member member) {
        if (member == null || member.getId() == null) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        if (data.getMember() == null || !data.getMember().getId().equals(member.getId())) {
        }
    }

    // 글 생성
    @Transactional
    public FaqQuestionResponseDto createQuestion(Long memberId, FaqQuestionRequestDto rqdto) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        // 중복 체크
        if(faqQuestionRepository.existsByTitle((rqdto.getTitle()))) {
            throw new MoaException(MoaExceptionType.ALREADY_QUESTION);
        }

        // 엔티티 생성
        FaqQuestion faqQuestion = FaqQuestion.builder().title(rqdto.getTitle()).contents(rqdto.getContent())
                .faqType(rqdto.getOption()).member(member).build();

        checkOwner(faqQuestion, member);

        // 저장
        FaqQuestion savedQuestionData = faqQuestionRepository.save(faqQuestion);

        return FaqQuestionResponseDto.fromEntity(savedQuestionData);
    }

    // 글 조회
    @Transactional(readOnly = true)
    public Page<FaqQuestionResponseDto> readQuestionList(Long memberId, int pageNo, String criteria) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        AuthValidator.checkAuthenticated(member);
        Pageable pageable = PageRequest.of(pageNo, PAGE_SIZE, Sort.by(Sort.Direction.DESC, criteria));
        Page<FaqQuestionResponseDto> page = faqQuestionRepository.findAll(pageable).map(FaqQuestionResponseDto::fromEntity);
        return page;
    }

    // 글 수정
    @Transactional
    public FaqQuestionResponseDto updateQuestion(Long memberId, Long id, FaqQuestionRequestDto rqdto, MultipartFile File) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        // 기존의 엔티티 조회
        FaqQuestion faqQuestionById = faqQuestionRepository.findById(id).orElseThrow(() ->
            new MoaException(MoaExceptionType.NOT_FOUND)
        );

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


        return FaqQuestionResponseDto.fromEntity(faqQuestionById);
    }

    // 글 삭제
    @Transactional
    public FaqQuestionResponseDto deleteQuestion(Long memberId, Long id) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        FaqQuestion questionToDelete = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));

        checkOwner(questionToDelete, member);

        faqQuestionRepository.delete(questionToDelete);

        return FaqQuestionResponseDto.fromEntity(questionToDelete);
    }

    // 상세 조회
    @Transactional
    public FaqQuestionResponseDto getDetailQuestion(Long memberId, Long id) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        FaqQuestion detailQuestion = faqQuestionRepository.findById(id)
                                        .orElseThrow(() -> new MoaException(MoaExceptionType.NOT_FOUND));
        checkOwner(detailQuestion, member);

        return FaqQuestionResponseDto.fromEntity(detailQuestion);
    }
}
