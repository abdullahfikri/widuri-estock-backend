package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierGetListResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;
import dev.mfikri.widuriestock.model.supplier.SupplierUpdateRequest;
import org.springframework.data.domain.Page;

public interface SupplierService {
    SupplierResponse create(SupplierCreateRequest request);
    Page<SupplierGetListResponse> getList(Integer page, Integer size);
    SupplierResponse get(Integer id);

    SupplierResponse update(SupplierUpdateRequest request);

    void delete(Integer id);
}
