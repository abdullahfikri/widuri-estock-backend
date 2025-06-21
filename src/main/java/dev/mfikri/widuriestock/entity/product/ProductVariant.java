package dev.mfikri.widuriestock.entity.product;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductVariantDetail;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String sku;
    private Integer stock;
    private Integer price;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.REMOVE)
    private List<ProductVariantAttribute> productVariantAttributes;

    @OneToMany(mappedBy = "productVariant")
    private List<IncomingProductVariantDetail> incomingProductVariantDetails;
}
