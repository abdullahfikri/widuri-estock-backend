package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.incoming_product.IncomingProductCreateRequest;
import dev.mfikri.widuriestock.model.incoming_product.IncomingProductResponse;
import dev.mfikri.widuriestock.service.IncomingProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

}
