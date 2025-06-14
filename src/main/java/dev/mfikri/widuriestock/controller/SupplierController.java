package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
        SupplierResponse response = supplierService.create(request);

        return WebResponse.<SupplierResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/suppliers/{supplierId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<SupplierResponse> get(@PathVariable Integer supplierId) {
        SupplierResponse response = supplierService.get(supplierId);

        return WebResponse.<SupplierResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/suppliers/{supplierId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable Integer supplierId) {
        supplierService.delete(supplierId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


}
