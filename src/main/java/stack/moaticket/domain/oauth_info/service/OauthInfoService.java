package stack.moaticket.domain.oauth_info.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepository;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepositoryQueryDsl;
import stack.moaticket.domain.member.entity.Member;

@Service
@RequiredArgsConstructor
public class OauthInfoService {

    private final OauthInfoRepository oauthInfoRepository;
    private final OauthInfoRepositoryQueryDsl oauthInfoRepositoryQueryDsl;

    public OauthInfo joinOauthInfo(Member member, String oauthId){
        OauthInfo oauthInfo = OauthInfo.builder()
                .member(member)
                .oauthId(oauthId)
                .build();
        return oauthInfoRepository.save(oauthInfo);
    }

    public OauthInfo findOauthInfo(String oauthId){
        return oauthInfoRepositoryQueryDsl.findByOauthId(oauthId);
    }

}
