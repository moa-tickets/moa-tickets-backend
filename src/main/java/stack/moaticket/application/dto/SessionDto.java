package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.session.entity.Session;

import java.time.LocalDateTime;

public abstract class SessionDto {

    @Getter
    @NoArgsConstructor
    public static class SessionRequest{
        private LocalDateTime date;
        private int price;

        public Session toEntity(Concert concert){
            return Session.builder()
                    .concert(concert)
                    .date(this.date)
                    .price(this.price)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SessionResponse{
        private Long sessionId;
        private LocalDateTime date;
        private int price;

        public static SessionResponse from(Session session){
            return SessionResponse.builder()
                    .sessionId(session.getId())
                    .date(session.getDate())
                    .price(session.getPrice())
                    .build();
        }
    }
}
