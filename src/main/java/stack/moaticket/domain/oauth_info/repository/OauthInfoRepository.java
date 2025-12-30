package stack.moaticket.domain.oauth_info.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.oauth_info.entity.OauthInfo;

@Repository
public interface OauthInfoRepository extends JpaRepository<OauthInfo, Long> {
}
