package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.ConcertDetailDto;
import stack.moaticket.application.dto.ConcertListDto;
import stack.moaticket.application.dto.CreateConcertDto;
import stack.moaticket.application.service.ProductService;
import stack.moaticket.domain.concert.service.ConcertService;
import stack.moaticket.domain.member.entity.Member;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    @PostMapping("/concert")
    public ResponseEntity<CreateConcertDto.Response> concertSave(
            @AuthenticationPrincipal Member member,
            @RequestBody CreateConcertDto.Request request){
        CreateConcertDto.Response response = productService.createConcert(member, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/detail/{id}")
    public ResponseEntity<ConcertDetailDto.Response> getConcertDetail(@PathVariable Long id){
        ConcertDetailDto.Response response = productService.getConcertDetail(id);   //TODO

        return ResponseEntity.ok(response);
    }

    @GetMapping("/concertList")
    public ResponseEntity<List<ConcertListDto.Response>> getConcertList(
            @RequestParam(value = "searchValue", required = false, defaultValue = "") String searchValue,
            @RequestParam(value = "sortBy", required = false, defaultValue = "date") String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable){

        return ResponseEntity.ok(productService.getConcertList(searchValue, sortBy, sortOrder, pageable));
    }



}
