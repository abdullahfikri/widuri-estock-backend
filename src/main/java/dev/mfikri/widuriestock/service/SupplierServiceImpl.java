package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.model.address.AddressResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierGetListResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierUpdateRequest;
import dev.mfikri.widuriestock.repository.AddressRepository;
import dev.mfikri.widuriestock.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SupplierServiceImpl implements SupplierService {
    private final ValidationService validationService;
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;
    private final AddressService addressService;

    public SupplierServiceImpl(ValidationService validationService, SupplierRepository supplierRepository, AddressRepository addressRepository, AddressService addressService) {
        this.validationService = validationService;
        this.supplierRepository = supplierRepository;
        this.addressRepository = addressRepository;
        this.addressService = addressService;
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
        addressService.setAddress(address,
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


        return toSupplierResponse(supplier, address);
    }

    @Override
    public Page<SupplierGetListResponse> getList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("supplierName")));

        Page<Supplier> supplierPage = supplierRepository.findAll(pageable);

        List<SupplierGetListResponse> supplierListResponse = supplierPage.getContent().stream().map(supplier -> SupplierGetListResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .build()).toList();

        return new PageImpl<>(supplierListResponse, pageable, supplierPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse get(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id cannot be null.");
        }

        Supplier supplier = findSupplierByIdOrThrows(id);

        return toSupplierResponse(supplier, supplier.getAddress());
    }

    @Override
    @Transactional
    public SupplierResponse update(SupplierUpdateRequest request) {
        log.info("Processing request to update a supplier. supplierId={}", request.getSupplierId());
        validationService.validate(request);

        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());

        Address address = supplier.getAddress();

        if (address.getId() != request.getAddress().getId()) {
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address is not found.");
        }

        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setInformation(request.getInformation());
        supplierRepository.save(supplier);

        addressService.setAddress(
                address,
                request.getAddress().getStreet(),
                request.getAddress().getVillage(),
                request.getAddress().getDistrict(),
                request.getAddress().getCity(),
                request.getAddress().getProvince(),
                request.getAddress().getCountry(),
                request.getAddress().getPostalCode()
        );


        return toSupplierResponse(supplier, address);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id cannot be null.");
        }

        Supplier supplier = findSupplierByIdOrThrows(id);
        addressRepository.deleteBySupplier(supplier);
        supplierRepository.delete(supplier);
    }

    private Supplier findSupplierByIdOrThrows(Integer id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found."));
    }

    private SupplierResponse toSupplierResponse(Supplier supplier, Address address) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .address(AddressServiceImpl.toAddressResponse(address))
                .build();
    }
}
