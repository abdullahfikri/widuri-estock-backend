package dev.mfikri.widuriestock.entity.product;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductDetail;
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
@Entity(name = "products")
@EntityListeners({AuditingEntityListener.class})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;

    @Column(name = "has_variant")
    private Boolean hasVariant;
    private Integer stock;
    private Integer price;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<ProductPhoto> productPhotos;

    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE)
    private List<ProductVariant> productVariants;

    @OneToMany(mappedBy = "product")
    private List<IncomingProductDetail> incomingProductDetails;
}

