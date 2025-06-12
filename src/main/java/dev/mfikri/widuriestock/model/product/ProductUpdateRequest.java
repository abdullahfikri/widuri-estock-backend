package dev.mfikri.widuriestock.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductUpdateRequest {

    @NotNull
    private Integer id;

    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @NotBlank
    @Size(min = 3)
    private String description;

    @NotNull
    @PositiveOrZero
    private Integer categoryId;

    @PositiveOrZero
    @Max(30000)
    private Integer stock;

    @PositiveOrZero
    private Integer price;

    @NotNull
    private Boolean hasVariant;

    @Valid
    private List<ProductVariantUpdateRequest> variants;

    @Valid
    private List<ProductPhotoUpdateRequest> productPhotos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductVariantUpdateRequest {
        @PositiveOrZero
        private Integer id;

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
        private List<ProductVariantAttributeUpdateRequest> attributes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static  class ProductVariantAttributeUpdateRequest {
        @PositiveOrZero
        private Integer id;

        @NotBlank
        @Size(min = 1, max = 100)
        private String attributeKey;
        @NotBlank
        @Size(min = 1, max = 100)
        private String attributeValue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductPhotoUpdateRequest {


        private String id;
//        @NotBlank
        private String imageLocation;
//        @NotNull
        private MultipartFile image;
    }
}
