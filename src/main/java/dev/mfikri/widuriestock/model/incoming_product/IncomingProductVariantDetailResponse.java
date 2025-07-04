package dev.mfikri.widuriestock.model.incoming_product;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductVariantDetailResponse {
    private Integer id;
    private IncomingProductProductVariantResponse variant;
    private Integer pricePerUnit;
    private Integer quantity;
    private Integer totalPrice;
}
