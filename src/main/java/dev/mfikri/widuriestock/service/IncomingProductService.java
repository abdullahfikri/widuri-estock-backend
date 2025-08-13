package dev.mfikri.widuriestock.service;


import dev.mfikri.widuriestock.model.incoming_product.*;
import org.springframework.data.domain.Page;

public interface IncomingProductService {
    IncomingProductResponse create(IncomingProductCreateRequest request);
    IncomingProductResponse get(Integer incomingProductId);
    Page<IncomingProductGetListResponse> getList(IncomingProductGetListRequest request);
    IncomingProductResponse update(IncomingProductUpdateRequest request);

    IncomingProductResponse.IncomingProductDetail addIncomingProductDetails(Integer incomingProductId, IncomingProductCreateRequest.IncomingProductDetails request);

    IncomingProductResponse.IncomingProductVariantDetail addIncomingProductVariantDetails(Integer incomingProductDetailId, IncomingProductCreateRequest.IncomingProductVariantDetail request);

    void deleteIncomingProduct(Integer incomingProductId);
    void deleteIncomingProductDetails(Integer incomingProductDetailId);
    void deleteIncomingProductVariantDetails(Integer incomingProductVariantDetailId);
}
