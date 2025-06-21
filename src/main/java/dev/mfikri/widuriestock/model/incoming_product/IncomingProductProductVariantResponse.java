package dev.mfikri.widuriestock.model.incoming_product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductProductVariantResponse {
    private Integer id;
    private String sku;
}
