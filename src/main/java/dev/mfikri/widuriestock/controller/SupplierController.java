package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.PagingResponse;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierGetListResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierUpdateRequest;
import dev.mfikri.widuriestock.service.SupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }


    @PostMapping(path = "/suppliers",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<SupplierResponse> create(@RequestBody SupplierCreateRequest request) {
        log.info("Receiving request to create a supplier.");

        SupplierResponse response = supplierService.create(request);

        return WebResponse.<SupplierResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/suppliers",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<SupplierGetListResponse>> getList(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                              @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        log.info("Receiving request to get list of suppliers.");

        Page<SupplierGetListResponse> responsePage = supplierService.getList(page, size);

        return WebResponse.<List<SupplierGetListResponse>>builder()
                .data(responsePage.getContent())
                .paging(PagingResponse.builder()
                        .currentPage(responsePage.getNumber())
                        .totalPage(responsePage.getTotalPages())
                        .sizePerPage(responsePage.getSize())
                        .build())
                .build();
    }



    @GetMapping(path = "/suppliers/{supplierId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<SupplierResponse> get(@PathVariable Integer supplierId) {
        log.info("Receiving request to get a supplier. supplierId={}", supplierId);

        SupplierResponse response = supplierService.get(supplierId);

        return WebResponse.<SupplierResponse>builder()
                .data(response)
                .build();
    }

    @PutMapping(path = "/suppliers/{supplierId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<SupplierResponse> update(@RequestBody SupplierUpdateRequest request, @PathVariable Integer supplierId) {
        log.info("Receiving request to update a supplier. supplierId={}", supplierId);

        request.setSupplierId(supplierId);

        SupplierResponse response = supplierService.update(request);

        return WebResponse.<SupplierResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/suppliers/{supplierId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable Integer supplierId) {
        log.info("Receiving request to delete a supplier. supplierId={}", supplierId);

        supplierService.delete(supplierId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


}
