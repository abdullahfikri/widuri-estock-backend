package dev.mfikri.widuriestock.model.incoming_product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductResponse {
    private Integer id;
    private LocalDate dateIn;
    private IncomingProductSupplierResponse supplier;
    private String username;
    private Integer totalProduct;
    private String note;

    private List<IncomingProductDetail> incomingProductDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductDetail {
        private Integer id;
        private IncomingProductProductResponse product;
        private Integer pricePerUnit;
        private Integer quantity;
        private Integer totalPrice;
        private Boolean hasVariant;
        private Integer totalVariantQuantity;
        private Integer totalVariantPrice;
        private List<IncomingProductVariantDetail> incomingProductVariantDetails;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductVariantDetail {
        private Integer id;
        private IncomingProductProductVariantResponse variant;
        private Integer pricePerUnit;
        private Integer quantity;
        private Integer totalPrice;
    }
}
