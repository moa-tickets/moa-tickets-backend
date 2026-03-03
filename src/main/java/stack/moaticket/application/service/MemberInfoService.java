package stack.moaticket.application.service;

import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberInfoService {
    private final Validator validator;

    private final MemberService memberService;

    public GetMemberDto.Response getMember(Long memberId){
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        return GetMemberDto.Response.from(member);
    }
}
