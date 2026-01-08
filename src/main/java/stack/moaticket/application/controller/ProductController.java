package stack.moaticket.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import stack.moaticket.domain.member.entity.Member;

import java.util.List;

@Tag(name = "Product API", description = "상품(콘서트)예약 도메인 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    @Operation(
            summary = "콘서트 Create",
            description = "콘서트와 세션 List를 받아 create 후 반환",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "콘서트, 세션 list 데이터",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateConcertDto.Request.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CreateConcertDto.Response.class)
                            )
                    )
            }
    )
    @PostMapping("/concert")
    public ResponseEntity<CreateConcertDto.Response> concertSave(
            @AuthenticationPrincipal Member member,
            @RequestBody CreateConcertDto.Request request){
        CreateConcertDto.Response response = productService.createConcert(member, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            security = {},
            summary = "콘서트 Read",
            description = "콘서트 id를 받아 해당하는 콘서트 조회",
            parameters = {
                @Parameter(
                        name = "id",
                        description = "조회할 콘서트 id",
                        required = true,
                        in = ParameterIn.PATH
                )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ConcertDetailDto.Response.class)
                            )
                    )
            }
    )
    @GetMapping("/detail/{id}")
    public ResponseEntity<ConcertDetailDto.Response> getConcertDetail(@PathVariable Long id){
        ConcertDetailDto.Response response = productService.getConcertDetail(id);   //TODO

        return ResponseEntity.ok(response);
    }



    @Operation(
            security = {},
            summary = "콘서트 list Read",
            description = "검색어와 정렬방식을 받으면 해당 조건에 맞게 list를 조회",
            parameters = {
                    @Parameter(
                            name = "searchValue",
                            description = "검색어",
                            required = false,
                            in = ParameterIn.QUERY
                    ),
                    @Parameter(
                            name = "sortBy",
                            description = "정렬 조건",
                            required = false,
                            in = ParameterIn.QUERY,
                            example = "date",
                            schema = @Schema(allowableValues = {"date"})
                    ),
                    @Parameter(
                            name = "sortOrder",
                            description = "정렬 방식",
                            required = false,
                            in = ParameterIn.QUERY,
                            example = "desc",
                            schema = @Schema(allowableValues = {"asc", "desc"})
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ConcertListDto.Response.class))
                            )
                    )
            }
    )
    @GetMapping("/concertList")
    public ResponseEntity<List<ConcertListDto.Response>> getConcertList(
            @RequestParam(value = "searchValue", required = false, defaultValue = "") String searchValue,
            @RequestParam(value = "sortBy", required = false, defaultValue = "date") String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable){

        return ResponseEntity.ok(productService.getConcertList(searchValue, sortBy, sortOrder, pageable));
    }



}
