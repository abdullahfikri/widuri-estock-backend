package dev.mfikri.widuriestock.entity.incoming_product;


import dev.mfikri.widuriestock.entity.product.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "incoming_product_variant_details")
@EntityListeners({AuditingEntityListener.class})
public class IncomingProductVariantDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price_per_unit")
    private Integer pricePerUnit;

    private Integer quantity;

    @Column(name = "total_price")
    private Integer totalPrice;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "incoming_product_detail_id", referencedColumnName = "id")
    private IncomingProductDetail incomingProductDetail;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", referencedColumnName = "id")
    private ProductVariant productVariant;

}
