package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.AddressCreateRequest;
import dev.mfikri.widuriestock.model.user.AddressResponse;
import dev.mfikri.widuriestock.model.user.AddressUpdateRequest;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ValidationService validationService;

    public AddressServiceImpl(UserRepository userRepository, AddressRepository addressRepository, ValidationService validationService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.validationService = validationService;
    }

    @Override
    @Transactional
    public AddressResponse create(AddressCreateRequest request) {
        validationService.validate(request);

        User user = userRepository.findById(request.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));

        Address address = new Address();
        address.setUser(user);
        setAddress(address, request.getStreet(), request.getVillage(), request.getDistrict(), request.getCity(), request.getProvince(), request.getCountry(), request.getPostalCode());

        addressRepository.save(address);

        return toAddressResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getList(String username) {


        User user = findUserByUsernameOrThrows(username);
        List<Address> addresses = addressRepository.findAllByUser(user, Sort.by("updatedAt"));
//        List<Address> addresses = addressRepository.findAllByUser(user);

        for (Address address : addresses) {
            log.info(address.getStreet());
        }
        return addresses.stream().map(AddressServiceImpl::toAddressResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse get(String username, Integer addressId) {
        User user = findUserByUsernameOrThrows(username);
        Address address = findAddressByIdAndUserOrThrows(addressId, user);

        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse update(AddressUpdateRequest request) {
        validationService.validate(request);

        User user = findUserByUsernameOrThrows(request.getUsername());

        Address address = findAddressByIdAndUserOrThrows(request.getAddressId(), user);

        // update
        setAddress(address, request.getStreet(), request.getVillage(), request.getDistrict(), request.getCity(), request.getProvince(), request.getCountry(), request.getPostalCode());
        addressRepository.save(address);

        return toAddressResponse(address);
    }

    private void setAddress(Address address, String street, String village, String district, String city, String province, String country, String postalCode) {
        address.setStreet(street);
        address.setVillage(village);
        address.setDistrict(district);
        address.setCity(city);
        address.setProvince(province);
        address.setCountry(country);
        address.setPostalCode(postalCode);
    }

    @Override
    @Transactional
    public void delete(String username, Integer addressId) {
        User user = findUserByUsernameOrThrows(username);
        int totalRecordImpact = addressRepository.deleteAddressByIdAndUser(addressId, user);
        if (totalRecordImpact != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address is not found.");
        }
    }



    private User findUserByUsernameOrThrows(String username) {
        return userRepository.findById(username.trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));
    }

    private Address findAddressByIdAndUserOrThrows(Integer addressId, User user) {
        return addressRepository.findAddressByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address is not found."));
    }

    static AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .village(address.getVillage())
                .district(address.getDistrict())
                .city(address.getCity())
                .province(address.getProvince())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .usernameId(address.getUser().getUsername())
                .build();
    }
}
