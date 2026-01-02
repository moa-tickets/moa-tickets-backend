package stack.moaticket.system.oauth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OauthService extends DefaultOAuth2UserService {

    //로그인 처음 시작 loadUser를 통해 oAuth2User객체를 성공적으로 받게되면 Oauth2SuccessHandler로 리턴해줌
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest){
        return super.loadUser(userRequest);
    }

}
