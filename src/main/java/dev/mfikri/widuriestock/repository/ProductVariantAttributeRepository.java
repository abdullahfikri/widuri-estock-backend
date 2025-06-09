package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.ProductVariantAttribute;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantAttributeRepository extends CrudRepository<ProductVariantAttribute, Integer> {
}
