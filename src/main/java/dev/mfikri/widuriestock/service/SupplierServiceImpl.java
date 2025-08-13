package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Address;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierSummaryResponse;
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

@Slf4j
@Service
public class SupplierServiceImpl implements SupplierService {
    private final ValidationService validationService;
    private final SupplierRepository supplierRepository;
    private final AddressService addressService;

    public SupplierServiceImpl(ValidationService validationService, SupplierRepository supplierRepository, AddressService addressService) {
        this.validationService = validationService;
        this.supplierRepository = supplierRepository;
        this.addressService = addressService;
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        log.info("Processing request to create a new supplier.");
        validationService.validate(request);

        Supplier supplier = buildSupplierFromRequest(request);
        Address address = buildAddressFromRequest(request, supplier);

        supplier.setAddress(address);
        log.debug("Saving new supplier entity to the database.");
        supplierRepository.save(supplier);

        log.info("Successfully to create a new supplier. supplierId={}", supplier.getId());
        return toSupplierResponse(supplier, address);
    }

    private Supplier buildSupplierFromRequest (SupplierCreateRequest request) {
        log.debug("Building supplier from request. request={}", request);
        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setInformation(request.getInformation());
        return supplier;
    }

    private Address buildAddressFromRequest(SupplierCreateRequest request, Supplier supplier){
        log.debug("Building address from requestAddress. address={}", request.getAddress());
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

        return address;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierSummaryResponse> getList(Integer page, Integer size) {
        log.info("Processing request to get a list of suppliers. page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("supplierName")));

        Page<Supplier> supplierPage = supplierRepository.findAll(pageable);

        log.info("Successfully to get a list of suppliers. totalPage={}, totalItems={}", supplierPage.getTotalPages(), supplierPage.getTotalElements());
        return supplierPage.map(this::toSupplierSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse get(Integer id) {
        log.info("Processing request to get a supplier. supplierId={}", id);
        Supplier supplier = findSupplierByIdOrThrows(id);

        log.info("Successfully to get a supplier. supplierId={}", supplier.getId());
        return toSupplierResponse(supplier, supplier.getAddress());
    }

    @Override
    @Transactional
    public SupplierResponse update(SupplierUpdateRequest request) {
        log.info("Processing request to update a supplier. supplierId={}", request.getSupplierId());
        validationService.validate(request);

        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());
        Address address = supplier.getAddress();

        buildUpdatedSupplier(request, supplier);
        buildUpdatedAddress(request, address);

        log.info("Successfully to update a supplier. supplierId={}", request.getSupplierId());
        return toSupplierResponse(supplier, address);
    }

    private void buildUpdatedSupplier (SupplierUpdateRequest request, Supplier supplier) {
        log.debug("Building updated supplier from request.");
        supplier.setSupplierName(request.getSupplierName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setInformation(request.getInformation());
    }

    private void buildUpdatedAddress(SupplierUpdateRequest request, Address address) {
        log.debug("Building updated address from request.");

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

    }



    @Override
    @Transactional
    public void delete(Integer id) {
        log.info("Processing request to delete a supplier. supplierId={}", id);
        Supplier supplier = findSupplierByIdOrThrows(id);
        supplierRepository.delete(supplier);
        log.info("Successfully to delete a supplier. supplierId={}", supplier.getId());
    }

    private Supplier findSupplierByIdOrThrows(Integer id) {
        log.debug("Finding supplier by id. supplierId={}", id);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier Id cannot be null.");
        }
        return supplierRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found."));
    }

    private SupplierResponse toSupplierResponse(Supplier supplier, Address address) {
        log.debug("Building supplier response from supplier.");
        return SupplierResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .address(addressService.toAddressResponse(address))
                .build();
    }

    private SupplierSummaryResponse toSupplierSummaryResponse(Supplier supplier) {
        log.debug("Building supplier summary response from supplier.");
        return SupplierSummaryResponse.builder()
                .id(supplier.getId())
                .supplierName(supplier.getSupplierName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .information(supplier.getInformation())
                .build();
    }
}
