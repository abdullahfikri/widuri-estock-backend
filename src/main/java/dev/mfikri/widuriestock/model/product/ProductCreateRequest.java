package dev.mfikri.widuriestock.model.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductCreateRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank
    @Size(min = 3)
    private String description;

    @NotNull
    @PositiveOrZero
    private Integer categoryId;

    @NotNull
    private Boolean hasVariant;

    @PositiveOrZero
    @Max(30000)
    private Integer stock;

    @PositiveOrZero
    private Integer price;

    @Valid
    private List<ProductPhoto> productPhotos;

    @Valid
    private List<ProductVariant> variants;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductPhoto {
        @NotNull
        private MultipartFile image;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductVariant {

        @NotBlank
        @Size(min = 1, max = 255)
        private String sku;

        @NotNull
        @PositiveOrZero
        @Max(30000)
        private Integer stock;

        @NotNull
        @PositiveOrZero
        private Integer price;

        @NotNull
        @Valid
        private List<ProductVariantAttribute> attributes;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ProductVariantAttribute {

            @NotBlank
            @Size(min = 1, max = 100)
            private String attributeKey;

            @NotBlank
            @Size(min = 1, max = 100)
            private String attributeValue;
        }
    }

}
