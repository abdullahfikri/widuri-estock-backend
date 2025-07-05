package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.PagingResponse;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.incoming_product.*;
import dev.mfikri.widuriestock.service.IncomingProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IncomingProductController {

    private final IncomingProductService incomingProductService;

    public IncomingProductController(IncomingProductService incomingProductService) {
        this.incomingProductService = incomingProductService;
    }

    @PostMapping(path = "/incoming-products",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<IncomingProductResponse> create(@RequestBody IncomingProductCreateRequest request, HttpServletRequest httpServletRequest) {
        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        request.setUsername(userPrincipal.getName());

        IncomingProductResponse response = incomingProductService.create(request);

        return WebResponse.<IncomingProductResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/incoming-products/{incomingProductId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<IncomingProductResponse> get(@PathVariable Integer incomingProductId) {
        IncomingProductResponse response = incomingProductService.get(incomingProductId);

        return WebResponse.<IncomingProductResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/incoming-products",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<IncomingProductGetListResponse>> getList(@RequestParam(value = "start_date", required = false)LocalDate startDate,
                                                                     @RequestParam(value = "end_date", required = false)LocalDate endDate,
                                                                     @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                     @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Page<IncomingProductGetListResponse> responsePage = incomingProductService.getList(IncomingProductGetListRequest.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .page(page)
                        .size(size)
                .build());
        return WebResponse.<List<IncomingProductGetListResponse>>builder()
                .data(responsePage.getContent())
                .paging(PagingResponse.builder()
                        .currentPage(responsePage.getNumber())
                        .totalPage(responsePage.getTotalPages())
                        .sizePerPage(responsePage.getSize())
                        .build())
                .build();
    }

    @PutMapping(path = "/incoming-products/{incomingProductId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<IncomingProductResponse> update(@RequestBody IncomingProductUpdateRequest request,
                                                       @PathVariable Integer incomingProductId,
                                                       HttpServletRequest httpServletRequest) {
        request.setId(incomingProductId);
        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        request.setUsername(userPrincipal.getName());

        IncomingProductResponse response = incomingProductService.update(request);

        return WebResponse.<IncomingProductResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping(path = "/incoming-products/{incomingProductId}/incoming-product-details",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<IncomingProductDetailResponse> createIncomingProductDetail(@PathVariable Integer incomingProductId,
                                                                                  @RequestBody IncomingProductDetailCreateRequest request
                                                                               ) {
        request.setIncomingProductId(incomingProductId);
        IncomingProductDetailResponse response = incomingProductService.createIncomingProductDetails(request);

        return WebResponse.<IncomingProductDetailResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping(path = "/incoming-product-details/{incomingProductDetailId}/incoming-product-variant-detail",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<IncomingProductVariantDetailResponse> createIncomingProductVariantDetail(@PathVariable Integer incomingProductDetailId,
                                                                                  @RequestBody IncomingProductVariantDetailCreateRequest request
    ) {
        request.setIncomingProductDetailId(incomingProductDetailId);
        IncomingProductVariantDetailResponse response = incomingProductService.createIncomingProductVariantDetails(request);

        return WebResponse.<IncomingProductVariantDetailResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/incoming-product-variant-details/{incomingProductVariantDetailId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProductVariantDetail(@PathVariable Integer incomingProductVariantDetailId) {
        incomingProductService.deleteIncomingProductVariantDetails(incomingProductVariantDetailId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }

    @DeleteMapping(path = "/incoming-product-details/{incomingProductDetailId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProductDetail(@PathVariable Integer incomingProductDetailId) {
        incomingProductService.deleteIncomingProductDetails(incomingProductDetailId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


    @DeleteMapping(path = "/incoming-products/{incomingProductId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProduct(@PathVariable Integer incomingProductId) {
        incomingProductService.deleteIncomingProduct(incomingProductId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }

}
