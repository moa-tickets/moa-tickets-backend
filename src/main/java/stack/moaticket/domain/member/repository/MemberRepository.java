package stack.moaticket.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.member.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

}
