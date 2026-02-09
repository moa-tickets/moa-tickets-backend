package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.RecommentDto;
import stack.moaticket.domain.comment.entity.Comment;
import stack.moaticket.domain.comment.repository.CommentRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.recomment.entity.Recomment;
import stack.moaticket.domain.recomment.repository.RecommentRepository;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class RecommentService {

    private final Validator validator;
    private final RecommentRepository recommentRepository;
    private final MemberService memberService;
    private final CommentRepository commentRepository;

    public List<RecommentDto.RecommentResponse> reads() {
        List<Recomment> recommentList = recommentRepository.findAll();
        List<RecommentDto.RecommentResponse> res = new ArrayList<>();

        for (Recomment r : recommentList) {
            res.add(this.entityToResponse(r));
        }
        return res;

    }

    public RecommentDto.RecommentResponse read(Long memberId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Recomment recommentEntity = recommentRepository.findById(memberId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        return entityToResponse(recommentEntity);
    }

    public void create(Long memberId, RecommentDto.Request request, Long commentId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        Recomment recomment = requestToEntity(member, request, comment);
        recommentRepository.save(recomment);
    }

    public void fix(Long memberId, RecommentDto.RecommentFixRequest recommentFixRequest, Long recommentId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Recomment recommentEntity = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        if (!recommentEntity.getRecommenter().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
        recommentEntity.fix(recommentFixRequest);
        recommentRepository.save(recommentEntity);
    }

    public void delete(Long memberId, Long recommentId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Recomment recommentEntity = recommentRepository.findById(recommentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.MEMBER_NOT_FOUND));

        if (!recommentEntity.getCommenter().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        recommentRepository.deleteById(recommentId);
    }


    private RecommentDto.RecommentResponse entityToResponse(Recomment recomment) {
        return RecommentDto.RecommentResponse.builder()
                .recommentId(recomment.getId())
                .commentId(recomment.getCommenter().getId())
                .nickName(recomment.getRecommenter().getNickname())
                .content(recomment.getContent())
                .build();
    }

    private Recomment requestToEntity(Member member, RecommentDto.Request request, Comment comment) {
        return Recomment.builder()
                .content(request.getContent())
                .recommenter(member)
                .commenter(comment)
                .build();
    }

}
