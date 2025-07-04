package dev.mfikri.widuriestock.model.incoming_product;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductDetailCreateRequest {
    @JsonIgnore
    @NotNull
    @PositiveOrZero
    private Integer incomingProductId;

    @NotNull
    @PositiveOrZero
    private Integer productId;

    @Positive
    private Integer pricePerUnit;

    @Positive
    private Integer quantity;

    @NotNull
    private Boolean hasVariant;

    @Valid
    List<IncomingProductVariantDetail> incomingProductVariantDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductVariantDetail {
        @NotNull
        @PositiveOrZero
        private Integer variantId;

        @NotNull
        @Positive
        private Integer pricePerUnit;

        @NotNull
        @Positive
        private Integer quantity;
    }
}

