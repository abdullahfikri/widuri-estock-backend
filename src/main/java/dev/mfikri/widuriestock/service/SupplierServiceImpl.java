package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {
    private final ValidationService validationService;
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;

    public SupplierServiceImpl(ValidationService validationService, SupplierRepository supplierRepository, AddressRepository addressRepository) {
        this.validationService = validationService;
        this.supplierRepository = supplierRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        validationService.validate(request);

        // check name duplicate
        boolean isSupplierNameExists = supplierRepository.existsBySupplierName(request.getSupplierName());
        if (isSupplierNameExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier name is already exists.");
        }

        // check email duplicate
        boolean isEmailExists = supplierRepository.existsByEmail(request.getEmail());
        if (isEmailExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already exists.");
        }

        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setInformation(request.getInformation());
        supplierRepository.save(supplier);

        Address address = new Address();
        AddressServiceImpl.setAddress(address,
                request.getAddress().getStreet(),
                request.getAddress().getVillage(),
                request.getAddress().getDistrict(),
                request.getAddress().getCity(),
                request.getAddress().getProvince(),
                request.getAddress().getCountry(),
                request.getAddress().getPostalCode()
        );
        address.setSupplier(supplier);
        addressRepository.save(address);


        return SupplierResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .addresses(Set.of(AddressResponse.builder()
                                .id(address.getId())
                                .street(address.getStreet())
                                .village(address.getVillage())
                                .district(address.getDistrict())
                                .city(address.getCity())
                                .province(address.getProvince())
                                .country(address.getCountry())
                                .postalCode(address.getPostalCode())
                        .build())
                )
                .build();
    }

    @Override
    public SupplierResponse get(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id cannot be null.");
        }

        Supplier supplier = findSupplierByIdOrThrows(id);

        Set<AddressResponse> addressResponses = supplier.getAddresses().stream().map(AddressServiceImpl::toAddressResponse).collect(Collectors.toSet());

        return SupplierResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .addresses(addressResponses)
                .build();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id cannot be null.");
        }

        Supplier supplier = findSupplierByIdOrThrows(id);
        addressRepository.deleteAllBySupplier(supplier);
        supplierRepository.delete(supplier);
    }

    private Supplier findSupplierByIdOrThrows(Integer id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found."));
    }
}
