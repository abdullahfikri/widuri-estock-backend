package dev.mfikri.widuriestock.model.incoming_product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductCreateRequest {

    @NotNull
    private LocalDate dateIn;

    @NotNull
    private Integer supplierId;

    @JsonIgnore
    @NotBlank
    @Size(max = 100)
    private String username;

    @NotNull
    private Integer totalProducts;

    @Size(max = 255)
    private String note;

    @NotNull
    @Valid
    private List<IncomingProductDetails> incomingProductDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IncomingProductDetails {


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
        private List<IncomingProductVariantDetail> incomingProductVariantDetails;
    }

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
