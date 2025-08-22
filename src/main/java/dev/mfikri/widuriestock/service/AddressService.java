package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.address.AddressUpdateRequest;

import java.util.List;

public interface AddressService {
    AddressResponse create(AddressCreateRequest request);
    List<AddressResponse> getList(String username);
    AddressResponse get(String username, Integer addressId);
    AddressResponse update(AddressUpdateRequest request);
    void delete(String username, Integer addressId);

    void setAddress(Address address, String street, String village, String district, String city, String province, String country, String postalCode);
    AddressResponse toAddressResponse(Address address);
}
