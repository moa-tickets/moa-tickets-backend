package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;

import java.time.LocalDateTime;
import java.util.List;

public class ConcertDto {

    //Request
    @Getter
    @NoArgsConstructor
    public static class ConcertRequest{
        private long memberId;
        private long hallId;
        private String concertName;
        private String concertDuration;
        private int concertAge;
        private LocalDateTime concertBookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String concertThumbnail;

        private List<SessionDto.SessionRequest> sessions;

        public Concert toEntity(Hall hall, Member member){
            return Concert.builder()
                    .hall(hall)
                    .member(member)
                    .name(this.concertName)
                    .duration(this.concertDuration)
                    .age(this.concertAge)
                    .bookingOpen(this.concertBookingOpen)
                    .start(this.concertStart)
                    .end(this.concertEnd)
                    .thumbnail(this.concertThumbnail)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ConcertResponse{
        private long concertId;
        private long memberId;
        private String memberName;
        private String hallName;
        private long hallId;
        private String concertName;
        private String concertDuration;
        private int concertAge;
        private LocalDateTime concertBookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String concertThumbnail;

        public static ConcertResponse from(Concert concert){
            return ConcertResponse.builder()
                    .concertId(concert.getId())
                    .memberId(concert.getMember().getId())
                    .memberName(concert.getMember().getNickname())
                    .hallName(concert.getHall().getName())
                    .hallId(concert.getHall().getId())
                    .concertName(concert.getName())
                    .concertDuration(concert.getDuration())
                    .concertAge(concert.getAge())
                    .concertBookingOpen(concert.getBookingOpen())
                    .concertStart(concert.getStart())
                    .concertEnd(concert.getEnd())
                    .concertThumbnail(concert.getThumbnail())
                    .build();
        }
    }
    //
    @Getter
    @Builder
    public static class ConcertDetailResponse{
        private long concertId;
        private long memberId;
        private String memberName;
        private String hallName;
        private long hallId;
        private String concertName;
        private String concertDuration;
        private int concertAge;
        private LocalDateTime concertBookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String concertThumbnail;

        private List<Session> sessions;

        //hall이나 member를 이름만 보낼것인지
        public static ConcertDetailResponse from(Concert concert, List<Session> sessions){
            return ConcertDetailResponse.builder()
                    .concertId(concert.getId())
                    .memberId(concert.getMember().getId())
                    .memberName(concert.getMember().getNickname())
                    .hallName(concert.getHall().getName())
                    .hallId(concert.getHall().getId())
                    .concertName(concert.getName())
                    .concertDuration(concert.getDuration())
                    .concertAge(concert.getAge())
                    .concertBookingOpen(concert.getBookingOpen())
                    .concertStart(concert.getStart())
                    .concertEnd(concert.getEnd())
                    .concertThumbnail(concert.getThumbnail())
                    .sessions(sessions)
                    .build();
        }
    }

}
