package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingProductRepository extends JpaRepository<IncomingProduct, Integer> {
}
