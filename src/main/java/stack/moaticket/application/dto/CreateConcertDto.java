package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

public abstract class CreateConcertDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private long hallId;
        private String concertName;
        private String concertDuration;
        private int age;
        private LocalDateTime bookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String thumbnail;
        private String detail;
        private List<SessionDto.SessionRequest> sessions;

        public Concert toConcert(Member member, Hall hall){
            return Concert.builder()
                    .member(member)
                    .hall(hall)
                    .name(this.concertName)
                    .duration(this.concertDuration)
                    .age(this.age)
                    .bookingOpen(this.bookingOpen)
                    .start(this.concertStart)
                    .end(this.concertEnd)
                    .detail(this.detail)
                    .thumbnail(this.thumbnail)
                    .build();
        }
    }
    @Getter
    @Builder
    public static class Response {
        private long concertId;
        private String concertName;
        private String memberName;
        private String concertDuration;
        private int age;
        private LocalDateTime bookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String thumbnail;
        private String detail;
        private List<SessionDto.SessionResponse> sessions;

        public static Response from(Concert concert, List<SessionDto.SessionResponse> sessions){
            return Response.builder()
                    .concertId(concert.getId())
                    .concertName(concert.getName())
                    .memberName(concert.getMember().getNickname())
                    .concertDuration(concert.getDuration())
                    .age(concert.getAge())
                    .bookingOpen(concert.getBookingOpen())
                    .concertStart(concert.getStart())
                    .concertEnd(concert.getEnd())
                    .thumbnail(concert.getThumbnail())
                    .sessions(sessions)
                    .detail(concert.getDetail())
                    .build();
        }
    }
}
