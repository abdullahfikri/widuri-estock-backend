package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.user.AddressCreateRequest;
import dev.mfikri.widuriestock.model.user.AddressResponse;
import dev.mfikri.widuriestock.model.user.AddressUpdateRequest;

import java.util.List;

public interface AddressService {
    AddressResponse create(AddressCreateRequest request);
    List<AddressResponse> getList(String username);
    AddressResponse get(String username, Integer addressId);
    AddressResponse update(AddressUpdateRequest request);
    void delete(String username, Integer addressId);
}
