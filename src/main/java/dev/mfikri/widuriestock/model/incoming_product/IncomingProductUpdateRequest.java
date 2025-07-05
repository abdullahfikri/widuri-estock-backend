package dev.mfikri.widuriestock.model.incoming_product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class IncomingProductUpdateRequest {
    @NotNull
    @JsonIgnore
    @PositiveOrZero
    private Integer id;

    @NotNull
    private LocalDate dateIn;

    @NotNull
    @PositiveOrZero
    private Integer supplierId;

    @JsonIgnore
    @NotBlank
    private String username;

    @NotNull
    @Positive
    private Integer totalProducts;

    private String note;

    @NotBlank
    private String updateReason;

    @NotNull
    @Valid
    private List<IncomingProductDetail> incomingProductDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductDetail {

        @NotNull
        @PositiveOrZero
        private Integer id;

        @NotNull
        @PositiveOrZero
        private Integer productId;

        @PositiveOrZero
        private Integer pricePerUnit;

        @Positive
        private Integer quantity;


        @NotNull
        private Boolean hasVariant;

        @JsonIgnore
        @Null
        private Integer totalVariantQuantity;

        @JsonIgnore
        @Null
        private Integer totalVariantPrice;

        @Valid
        private List<IncomingProductVariantDetail> incomingProductVariantDetails;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductVariantDetail {
        @PositiveOrZero
        @NotNull
        private Integer id;

        @NotNull
        @PositiveOrZero
        private Integer variantId;

        @NotNull
        @PositiveOrZero
        private Integer pricePerUnit;

        @NotNull
        @PositiveOrZero
        private Integer quantity;
    }

}
