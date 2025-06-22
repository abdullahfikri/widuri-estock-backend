package dev.mfikri.widuriestock.service;


import dev.mfikri.widuriestock.model.incoming_product.IncomingProductCreateRequest;
import dev.mfikri.widuriestock.model.incoming_product.IncomingProductResponse;

public interface IncomingProductService {
    IncomingProductResponse create(IncomingProductCreateRequest request);
    IncomingProductResponse get(Integer incomingProductId);
}
