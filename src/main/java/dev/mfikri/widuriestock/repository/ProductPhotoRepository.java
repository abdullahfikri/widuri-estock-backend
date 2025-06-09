package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.ProductPhoto;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPhotoRepository extends CrudRepository<ProductPhoto, Integer> {
}
