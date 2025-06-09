package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.PagingResponse;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.ProductCreateRequest;
import dev.mfikri.widuriestock.model.product.ProductResponse;
import dev.mfikri.widuriestock.model.product.ProductsGetListResponse;
import dev.mfikri.widuriestock.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(path = "/products",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<ProductResponse> create(@ModelAttribute ProductCreateRequest request) {
        ProductResponse response = productService.create(request);

        return WebResponse.<ProductResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/products",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<ProductsGetListResponse>> getList(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                        @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Page<ProductsGetListResponse> responsePage = productService.getList(page, size);

        return WebResponse.<List<ProductsGetListResponse>>builder()
                .data(responsePage.getContent())
                .paging(PagingResponse.builder()
                        .currentPage(responsePage.getNumber())
                        .totalPage(responsePage.getTotalPages())
                        .sizePerPage(responsePage.getSize())
                        .build())
                .build();
    }

    @GetMapping(path = "/products/{productId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<ProductResponse> get(@PathVariable Integer productId) {
        ProductResponse response = productService.get(productId);

        return WebResponse.<ProductResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/products/{productId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable Integer productId) {
        productService.delete(productId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


}
