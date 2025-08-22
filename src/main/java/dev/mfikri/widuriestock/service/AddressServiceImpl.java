package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.address.AddressUpdateRequest;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
        log.info("Processing request to create a new address for user.");

        validationService.validate(request);

        log.debug("Finding user associated with the request.");
        User user = userRepository.findById(request.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));

        Address address = new Address();
        address.setUser(user);
        setAddress(address, request.getStreet(), request.getVillage(), request.getDistrict(), request.getCity(), request.getProvince(), request.getCountry(), request.getPostalCode());

        log.debug("Saving new address entity to the database.");
        addressRepository.save(address);

        log.info("Successfully created new address. addressId={}.", address.getId());
        return toAddressResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getList(String username) {
        log.info("Processing request to get list of addresses for user.");

        User user = findUserByUsernameOrThrows(username);

        log.debug("Finding all addresses associated with the user");
        List<Address> addresses = addressRepository.findAllByUser(user, Sort.by("updatedAt"));


        log.info("Successfully found addresses for the user. count={}", addresses.size());
        return addresses.stream().map(this::toAddressResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse get(String username, Integer addressId) {
        log.info("Processing request to get an address. addressId={}", addressId);

        User user = findUserByUsernameOrThrows(username);

        Address address = findAddressByIdAndUserOrThrows(addressId, user);

        log.info("Successfully found address. addressId={}", addressId);
        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse update(AddressUpdateRequest request) {
        log.info("Processing request to update an address. addressId={}", request.getAddressId());

        validationService.validate(request);

        User user = findUserByUsernameOrThrows(request.getUsername());

        Address address = findAddressByIdAndUserOrThrows(request.getAddressId(), user);

        // update
        setAddress(address, request.getStreet(), request.getVillage(), request.getDistrict(), request.getCity(), request.getProvince(), request.getCountry(), request.getPostalCode());

        log.debug("Saving updated address entity to the database.");
        addressRepository.save(address);

        log.info("Successfully update an address. addressId={}", address.getId());
        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public void delete(String username, Integer addressId) {
        log.info("Processing request to delete an address. addressId={}", addressId);

        User user = findUserByUsernameOrThrows(username);
        log.debug("Delete an address with combination of user and addressId.");

        Address address = findAddressByIdAndUserOrThrows(addressId, user);
        addressRepository.delete(address);


        log.info("Successfully delete an address. addressId={}", addressId);
    }

    public void setAddress(Address address, String street, String village, String district, String city, String province, String country, String postalCode) {
        address.setStreet(street);
        address.setVillage(village);
        address.setDistrict(district);
        address.setCity(city);
        address.setProvince(province);
        address.setCountry(country);
        address.setPostalCode(postalCode);
    }

    private User findUserByUsernameOrThrows(String username) {
        log.debug("Finding user. username={}", username);
        return userRepository.findById(username.trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));
    }

    private Address findAddressByIdAndUserOrThrows(Integer addressId, User user) {
        log.debug("Finding address. addressId={}", addressId);
        return addressRepository.findAddressByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address is not found."));
    }

    public AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .village(address.getVillage())
                .district(address.getDistrict())
                .city(address.getCity())
                .province(address.getProvince())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .usernameId(address.getUser() == null ? null : address.getUser().getUsername())
                .supplierId(address.getSupplier() == null ? null : address.getSupplier().getId())
                .build();
    }
}
