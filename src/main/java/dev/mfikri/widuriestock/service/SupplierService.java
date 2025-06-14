package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.supplier.SupplierCreateRequest;
import dev.mfikri.widuriestock.model.supplier.SupplierResponse;

public interface SupplierService {
    SupplierResponse create(SupplierCreateRequest request);
    SupplierResponse get(Integer id);
    void delete(Integer id);
}
