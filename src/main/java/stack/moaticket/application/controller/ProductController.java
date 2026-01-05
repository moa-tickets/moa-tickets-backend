package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.ConcertDto;
import stack.moaticket.application.dto.HallDto;
import stack.moaticket.application.service.ProductService;
import stack.moaticket.domain.concert.service.ConcertService;
import stack.moaticket.domain.hall.service.HallService;
import stack.moaticket.domain.session.service.SessionService;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.type.HallType;
import stack.moaticket.domain.session.entity.Session;

import java.util.List;

import static stack.moaticket.domain.concert.entity.QConcert.concert;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;
    private final ConcertService concertService;

    @PostMapping("/concert")
    public ResponseEntity<Long> concertSave(@RequestBody ConcertDto.ConcertRequest request){
        long concertId = productService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(concertId);
    }
//    @GetMapping("/concert")
//    public ResponseEntity<ConcertDto.ConcertResponse> searchConcerts(){
//
////        return ResponseEntity.ok(productService.search(condition, pageable));
//    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ConcertDto.ConcertDetailResponse> getConcertDetail(@PathVariable("id") Long id){
        ConcertDto.ConcertDetailResponse response = productService.getConcertDetail(id);
        return ResponseEntity.ok(response);
    }


//    @PostMapping("/hall")
//    public ResponseEntity<Long> hallSave(@RequestBody HallDto.HallRequest request){
////        long hallId = hallService.upsertHall("hallName", HallType.LARGE).getId();
////        return ResponseEntity.status(HttpStatus.CREATED).body(hallId);
//    }
}
