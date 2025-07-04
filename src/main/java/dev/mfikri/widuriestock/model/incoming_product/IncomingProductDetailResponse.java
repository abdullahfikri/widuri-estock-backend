package dev.mfikri.widuriestock.model.incoming_product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductDetailResponse {
    private Integer id;
    private IncomingProductProductResponse product;
    private Integer pricePerUnit;
    private Integer quantity;
    private Integer totalPrice;
    private Boolean hasVariant;
    private Integer totalVariantQuantity;
    private Integer totalVariantPrice;
    private List<IncomingProductVariantDetail> incomingProductVariantDetails;

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
