package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    void deleteAllByProduct(Product product);

    Optional<ProductVariant> findByIdAndProduct(Integer id, Product product);

    List<ProductVariant> findByProduct(Product product);

}
