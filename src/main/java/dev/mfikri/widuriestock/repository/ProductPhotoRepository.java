package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductPhoto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductPhotoRepository extends CrudRepository<ProductPhoto, String> {
    Optional<ProductPhoto> findByIdAndProduct(String id, Product product);

    void deleteAllByProduct(Product product);
}
