package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.concert.entity.Concert;

import java.time.LocalDateTime;
import java.util.List;

public abstract class ConcertDetailDto {

    @Getter
    @NoArgsConstructor
    public static class Request{

    }

    @Getter
    @Builder
    public static class Response{
        private long concertId;
        private String concertName;
        private String concertDuration;
        private int age;
        private LocalDateTime bookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String thumbnail;

        private List<SessionDto.SessionResponse> sessions;
        public static ConcertDetailDto.Response from(Concert concert, List<SessionDto.SessionResponse> sessions){
            return ConcertDetailDto.Response.builder()
                    .concertId(concert.getId())
                    .concertName(concert.getName())
                    .concertDuration(concert.getDuration())
                    .age(concert.getAge())
                    .bookingOpen(concert.getBookingOpen())
                    .concertStart(concert.getStart())
                    .concertEnd(concert.getEnd())
                    .thumbnail(concert.getThumbnail())
                    .sessions(sessions)
                    .build();
        }
    }
}
