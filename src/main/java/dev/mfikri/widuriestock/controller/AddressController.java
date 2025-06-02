package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.AddressCreateRequest;
import dev.mfikri.widuriestock.model.user.AddressResponse;
import dev.mfikri.widuriestock.model.user.AddressUpdateRequest;
import dev.mfikri.widuriestock.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

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
        Principal userPrincipal = httpServletRequest.getUserPrincipal();

        addressService.delete(userPrincipal.getName(), addressId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }


}
