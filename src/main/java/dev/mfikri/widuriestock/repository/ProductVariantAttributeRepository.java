package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.ProductVariant;
import dev.mfikri.widuriestock.entity.product.ProductVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantAttributeRepository extends JpaRepository<ProductVariantAttribute, Integer> {
    Optional<ProductVariantAttribute> findByIdAndProductVariant(Integer id, ProductVariant productVariant);
}
