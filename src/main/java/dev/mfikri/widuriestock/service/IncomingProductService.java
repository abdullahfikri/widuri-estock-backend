package dev.mfikri.widuriestock.service;


import dev.mfikri.widuriestock.model.incoming_product.*;
import org.springframework.data.domain.Page;

public interface IncomingProductService {
    IncomingProductResponse create(IncomingProductCreateRequest request);
    IncomingProductResponse get(Integer incomingProductId);
    Page<IncomingProductGetListResponse> getList(IncomingProductGetListRequest request);
    IncomingProductResponse update(IncomingProductUpdateRequest request);

    IncomingProductDetailResponse createIncomingProductDetails(IncomingProductDetailCreateRequest request);
    IncomingProductVariantDetailResponse createIncomingProductVariantDetails(IncomingProductVariantDetailCreateRequest request);

    void deleteIncomingProduct(Integer incomingProductId);
    void deleteIncomingProductDetails(Integer incomingProductDetailId);
    void deleteIncomingProductVariantDetails(Integer incomingProductVariantDetailId);
}
