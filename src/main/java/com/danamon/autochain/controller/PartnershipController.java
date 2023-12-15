package com.danamon.autochain.controller;

import com.danamon.autochain.dto.DataResponse;
import com.danamon.autochain.dto.PagingResponse;
import com.danamon.autochain.dto.company.SearchCompanyRequest;
import com.danamon.autochain.dto.partnership.PartnershipResponse;
import com.danamon.autochain.dto.partnership.SearchPartnershipRequest;
import com.danamon.autochain.service.PartnershipService;
import com.danamon.autochain.util.PagingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partnerships")
@RequiredArgsConstructor
//@SecurityRequirement(name = "Bearer Authentication")
public class PartnershipController {
    private final PartnershipService partnershipService;

    @GetMapping
    public ResponseEntity<?> getAllPartnerships(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false, defaultValue = "company") String sortBy
    ) {
        page = PagingUtil.validatePage(page);
        size = PagingUtil.validateSize(size);
        direction = PagingUtil.validateDirection(direction);

        SearchPartnershipRequest request = SearchPartnershipRequest.builder()
                .page(page)
                .size(size)
                .direction(direction)
                .sortBy(sortBy)
                .build();

        Page<PartnershipResponse> companyResponse = partnershipService.getAll(request);
        PagingResponse pagingResponse = PagingResponse.builder()
                .count(companyResponse.getTotalElements())
                .totalPages(companyResponse.getTotalPages())
                .page(page)
                .size(size)
                .build();
        DataResponse<List<PartnershipResponse>> response = DataResponse.<List<PartnershipResponse>>builder()
                .message("successfully get all partnership")
                .statusCode(HttpStatus.OK.value())
                .data(companyResponse.getContent())
                .paging(pagingResponse)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}