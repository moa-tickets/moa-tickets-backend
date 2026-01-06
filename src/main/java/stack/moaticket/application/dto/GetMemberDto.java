package stack.moaticket.application.dto;
import stack.moaticket.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

public abstract class GetMemberDto {

    @Getter
    @Builder
    public static class Response{
        private String nickname;
        private boolean isSeller;
        private String email;
    }
    public static Response from(Member member){
        return Response.builder()
                .nickname(member.getNickname())
                .isSeller(member.isSeller())
                .email(member.getEmail())
                .build();
    }
}
