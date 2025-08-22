package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.address.AddressUpdateRequest;
import dev.mfikri.widuriestock.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/users")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }


    // current
    @PostMapping(path = "/current/addresses",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<AddressResponse> createAddressForCurrentUser(@RequestBody AddressCreateRequest request, HttpServletRequest httpServletRequest) {
        log.info("Receiving request to create new address for current user.");

        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        request.setUsername(userPrincipal.getName());

        AddressResponse response = addressService.create(request);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/current/addresses",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<AddressResponse>> getListAddressForCurrentUser(HttpServletRequest httpServletRequest) {
        log.info("Receiving request to get all address for current user.");

        Principal userPrincipal = httpServletRequest.getUserPrincipal();

        List<AddressResponse> responses = addressService.getList(userPrincipal.getName());

        return WebResponse.<List<AddressResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping(path = "/current/addresses/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AddressResponse> getAddressForCurrentUser(@PathVariable Integer addressId, HttpServletRequest httpServletRequest) {
        log.info("Receiving request to get an address for current user, addressId={}.", addressId);

        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        AddressResponse response = addressService.get(userPrincipal.getName(), addressId);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }

    @PutMapping(path = "/current/addresses/{addressId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AddressResponse> updateAddressForCurrentUser(@RequestBody AddressUpdateRequest request, HttpServletRequest httpServletRequest, @PathVariable Integer addressId) {
        log.info("Receiving request to update an address for current user, addressId={}.", addressId);

        Principal userPrincipal = httpServletRequest.getUserPrincipal();

        request.setUsername(userPrincipal.getName());
        request.setAddressId(addressId);

        AddressResponse response = addressService.update(request);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }


    @DeleteMapping(path = "/current/addresses/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> deleteAddressForCurrentUser(HttpServletRequest httpServletRequest, @PathVariable Integer addressId) {
        log.info("Receiving request to delete an address for current user, addressId={}.", addressId);

        Principal userPrincipal = httpServletRequest.getUserPrincipal();

        addressService.delete(userPrincipal.getName(), addressId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


}
