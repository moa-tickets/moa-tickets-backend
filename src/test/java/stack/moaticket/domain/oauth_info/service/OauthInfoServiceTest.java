package stack.moaticket.domain.oauth_info.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepository;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.oauth_info.repository.OauthInfoRepositoryQueryDsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OauthInfoServiceTest {

    @Mock
    OauthInfoRepository oauthInfoRepository;
    @Mock
    OauthInfoRepositoryQueryDsl oauthInfoRepositoryQueryDsl;
    @InjectMocks
    OauthInfoService oauthInfoService;

    @DisplayName("joinOauthInfo")
    @Test
    void saveOauthInfo(){
        // given
        Member member = mock(Member.class);
        String oauthId = "oauthId";
        when(oauthInfoRepository.save(any(OauthInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // when
        OauthInfo saved = oauthInfoService.joinOauthInfo(member, oauthId);
        // then
        assertThat(saved.getOauthId()).isEqualTo(oauthId);
        assertThat(saved.getMember()).isSameAs(member);

        ArgumentCaptor<OauthInfo> captor = ArgumentCaptor.forClass(OauthInfo.class);
        verify(oauthInfoRepository, times(1)).save(captor.capture());

        OauthInfo toSave = captor.getValue();
        assertThat(toSave.getMember()).isSameAs(member);
        assertThat(toSave.getOauthId()).isEqualTo(oauthId);

        verifyNoInteractions(oauthInfoRepositoryQueryDsl);
    }

    @DisplayName("findOauthInfo 성공 테스트")
    @Test
    void findOauthInfoTest(){
        // given
        String oauthId = "oauthId";
        OauthInfo oauthInfo = mock(OauthInfo.class);
        when(oauthInfoRepositoryQueryDsl.findByOauthId(oauthId)).thenReturn(oauthInfo);
        // when
        OauthInfo found = oauthInfoService.findOauthInfo(oauthId);
        // then
        assertThat(found).isSameAs(oauthInfo);
        verify(oauthInfoRepositoryQueryDsl, times(1)).findByOauthId(oauthId);
        verifyNoInteractions(oauthInfoRepository);
    }

}