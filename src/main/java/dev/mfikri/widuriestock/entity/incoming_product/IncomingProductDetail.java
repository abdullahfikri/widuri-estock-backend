package dev.mfikri.widuriestock.entity.incoming_product;

import dev.mfikri.widuriestock.entity.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "incoming_product_details")
@EntityListeners({AuditingEntityListener.class})
public class IncomingProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price_per_unit")
    private Integer pricePerUnit;

    private Integer quantity;

    @Column(name = "total_price")
    private Integer totalPrice;

    @Column(name = "has_variant")
    private Boolean hasVariant;

    @Column(name = "total_variant_quantity")
    private Integer totalVariantQuantity;

    @Column(name = "total_variant_price")
    private Integer totalVariantPrice;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "incoming_product_id", referencedColumnName = "id")
    private IncomingProduct incomingProduct;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    @OneToMany(mappedBy = "incomingProductDetail", cascade = CascadeType.REMOVE)
    private List<IncomingProductVariantDetail> incomingProductVariantDetails;
}
