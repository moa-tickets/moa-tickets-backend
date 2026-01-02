package stack.moaticket.system.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepository;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepositoryQueryDsl;

@Service
@RequiredArgsConstructor
public class OauthService extends DefaultOAuth2UserService {

    private final OauthInfoRepository oauthInfoRepository;
    private final OauthInfoRepositoryQueryDsl oauthInfoRepositoryQueryDsl;

    //로그인 처음 시작 loadUser를 통해 oAuth2User객체를 성공적으로 받게되면 Oauth2SuccessHandler로 리턴해줌
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest){
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return oAuth2User;
    }

    public OauthInfo joinOauthInfo(OauthInfo oauthInfo){
        return oauthInfoRepository.save(oauthInfo);
    }

    public OauthInfo findOauthInfo(String oauthId){
        return oauthInfoRepositoryQueryDsl.findByOauthId(oauthId);
    }

}
