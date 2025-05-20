package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.AddressCreateRequest;
import dev.mfikri.widuriestock.model.user.AddressResponse;
import dev.mfikri.widuriestock.model.user.AddressUpdateRequest;
import dev.mfikri.widuriestock.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/api/users/{username}")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }


    @PostMapping(path = "/addresses",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<AddressResponse> create(@RequestBody AddressCreateRequest request, @PathVariable String username, User user) {
        request.setUsername(username);

        AddressResponse response = addressService.create(request);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/addresses",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<AddressResponse>> getList(@PathVariable String username, User user) {
        List<AddressResponse> responses = addressService.getList(username);

        return WebResponse.<List<AddressResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping(path = "/addresses/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AddressResponse> get(@PathVariable String username, @PathVariable Integer addressId, User user) {
        AddressResponse response = addressService.get(username, addressId);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }

    @PutMapping(path = "/addresses/{addressId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<AddressResponse> update(@RequestBody AddressUpdateRequest request, @PathVariable String username, @PathVariable Integer addressId, User user) {
        request.setUsername(username);
        request.setAddressId(addressId);

        AddressResponse response = addressService.update(request);

        return WebResponse.<AddressResponse>builder()
                .data(response)
                .build();
    }


    @DeleteMapping(path = "/addresses/{addressId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable String username, @PathVariable Integer addressId, User user) {
        addressService.delete(username, addressId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }
}
