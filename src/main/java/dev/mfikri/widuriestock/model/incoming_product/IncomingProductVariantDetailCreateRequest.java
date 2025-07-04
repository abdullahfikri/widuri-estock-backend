package dev.mfikri.widuriestock.model.incoming_product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductVariantDetailCreateRequest {

    @NotNull
    @JsonIgnore
    private Integer incomingProductDetailId;

    @NotNull
    private Integer variantId;

    @NotNull
    private Integer pricePerUnit;

    @NotNull
    private Integer quantity;

}
