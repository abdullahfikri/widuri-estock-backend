package dev.mfikri.widuriestock.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "product_variant_attributes")
public class ProductVariantAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "attribute_key")
    private String attributeKey;

    @Column(name = "attribute_value")
    private String attributeValue;

    @ManyToOne
    @JoinColumn(name = "product_variants_id", referencedColumnName = "id")
    private ProductVariant productVariant;
}
