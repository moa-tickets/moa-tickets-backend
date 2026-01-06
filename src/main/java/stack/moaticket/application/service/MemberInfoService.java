package stack.moaticket.application.service;

import stack.moaticket.application.dto.GetMemberDto;
import stack.moaticket.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberInfoService {
    public GetMemberDto.Response getMember(Member member){
        return GetMemberDto.Response.from(member);
    }
}
