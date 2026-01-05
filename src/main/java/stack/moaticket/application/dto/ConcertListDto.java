package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.concert.entity.Concert;

import java.time.LocalDateTime;

public abstract class ConcertListDto {


    @Getter
    @NoArgsConstructor
    public static class Request{
        private String concertName;
        private String sortBy = "date";
        private String sortOrder = "desc";

    }

    @Getter
    @Builder
    public static class Response{
        private String concertName;
        private String concertDuration;
        private LocalDateTime bookingOpen;
        private LocalDateTime concertStart;
        private LocalDateTime concertEnd;
        private String concertThumbnail;

        public static Response from(Concert concert){
            return Response.builder()
                    .concertName(concert.getName())
                    .concertDuration(concert.getDuration())
                    .bookingOpen(concert.getBookingOpen())
                    .concertStart(concert.getStart())
                    .concertEnd(concert.getEnd())
                    .concertThumbnail(concert.getThumbnail())
                    .build();
        }
    }
}
