package stack.moaticket.system.util;

import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

public final class AuthValidator {

    private AuthValidator() {}

    public static void checkAuthenticated(Member member) {
        if (member == null || member.getId() == null) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }
    }
}