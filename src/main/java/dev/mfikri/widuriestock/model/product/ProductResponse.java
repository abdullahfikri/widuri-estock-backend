package dev.mfikri.widuriestock.model.product;

import jakarta.persistence.Column;
import lombok.*;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private Integer id;
    private String name;
    private String description;
    private Category category;
    private Boolean hasVariant;
    private Integer stock;
    private Integer price;

    private List<ProductPhoto> photos;
    private List<ProductVariant> variants;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductVariant {
        private Integer id;
        private String sku;
        private Integer stock;
        private Integer price;

        List<ProductVariantAttribute> attributes;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductVariantAttribute {
        private Integer id;
        private String attributeKey;
        private String attributeValue;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ProductPhoto {
        private String imageLocation;
    }
}
