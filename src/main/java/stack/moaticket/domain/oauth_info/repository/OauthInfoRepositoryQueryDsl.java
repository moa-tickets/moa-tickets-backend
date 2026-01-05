package stack.moaticket.domain.oauth_info.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;

import static stack.moaticket.domain.oauth_info.entity.QOauthInfo.oauthInfo;
@Repository
@RequiredArgsConstructor
public class OauthInfoRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public OauthInfo findByOauthId(String oauthId){
        return jpaQueryFactory.selectFrom(oauthInfo)
                .where(oauthInfo.oauthId.eq(oauthId))
                .fetchOne();
    }
}
