package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductVariant;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends CrudRepository<ProductVariant, Integer> {
    boolean existsBySkuAndProduct(String sku, Product product);
}
