package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, String> {
    Optional<ProductPhoto> findByIdAndProduct(String id, Product product);

    void deleteAllByProduct(Product product);

    List<ProductPhoto> findProductPhotoByProduct(Product product);
}
