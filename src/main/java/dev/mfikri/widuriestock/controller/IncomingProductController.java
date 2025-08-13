package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.PagingResponse;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.incoming_product.*;
import dev.mfikri.widuriestock.service.IncomingProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
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
        log.info("Receiving request to create an incoming product.");

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
        log.info("Receiving request to get an incoming product. incomingProductId={}.", incomingProductId);


        IncomingProductResponse response = incomingProductService.get(incomingProductId);

        return WebResponse.<IncomingProductResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/incoming-products",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<IncomingProductGetListResponse>> getList(@ModelAttribute IncomingProductGetListRequest request) {
        log.info("Receiving request to get all incoming products.");

        Page<IncomingProductGetListResponse> responsePage = incomingProductService.getList(request);
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
        log.info("Receiving request to update an incoming product. incomingProductId={}.", incomingProductId);

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
    public WebResponse<IncomingProductResponse.IncomingProductDetail> createIncomingProductDetail(@PathVariable Integer incomingProductId,
                                                                                  @RequestBody IncomingProductCreateRequest.IncomingProductDetails request
                                                                               ) {
        log.info("Receiving request to create an incoming product detail for incoming product. incomingProductId={}.", incomingProductId);

        IncomingProductResponse.IncomingProductDetail response = incomingProductService.addIncomingProductDetails(incomingProductId, request);

        return WebResponse.<IncomingProductResponse.IncomingProductDetail>builder()
                .data(response)
                .build();
    }

    @PostMapping(path = "/incoming-product-details/{incomingProductDetailId}/incoming-product-variant-detail",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<IncomingProductResponse.IncomingProductVariantDetail> createIncomingProductVariantDetail(@PathVariable Integer incomingProductDetailId,
                                                                                  @RequestBody IncomingProductCreateRequest.IncomingProductVariantDetail request
    ) {
        log.info("Receiving request to create an incoming product variant detail for incoming product detail. incomingProductDetailId={}.", incomingProductDetailId);
        IncomingProductResponse.IncomingProductVariantDetail response = incomingProductService.addIncomingProductVariantDetails(incomingProductDetailId, request);

        return WebResponse.<IncomingProductResponse.IncomingProductVariantDetail>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/incoming-product-variant-details/{incomingProductVariantDetailId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProductVariantDetail(@PathVariable Integer incomingProductVariantDetailId) {
        log.info("Receiving request to delete an incoming product variant detail. incomingProductVariantDetailId={}.", incomingProductVariantDetailId);


        incomingProductService.deleteIncomingProductVariantDetails(incomingProductVariantDetailId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }

    @DeleteMapping(path = "/incoming-product-details/{incomingProductDetailId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProductDetail(@PathVariable Integer incomingProductDetailId) {
        log.info("Receiving request to delete an incoming product detail. incomingProductDetailId={}.", incomingProductDetailId);

        incomingProductService.deleteIncomingProductDetails(incomingProductDetailId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


    @DeleteMapping(path = "/incoming-products/{incomingProductId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteIncomingProduct(@PathVariable Integer incomingProductId) {
        log.info("Receiving request to delete an incoming product. incomingProductId={}.", incomingProductId);

        incomingProductService.deleteIncomingProduct(incomingProductId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }

}
