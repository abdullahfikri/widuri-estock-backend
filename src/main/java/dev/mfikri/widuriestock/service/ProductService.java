package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.product.ProductCreateRequest;
import dev.mfikri.widuriestock.model.product.ProductResponse;
import dev.mfikri.widuriestock.model.product.ProductsGetListResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    ProductResponse create(ProductCreateRequest request);
    Page<ProductsGetListResponse> getList(Integer page, Integer size);
}
