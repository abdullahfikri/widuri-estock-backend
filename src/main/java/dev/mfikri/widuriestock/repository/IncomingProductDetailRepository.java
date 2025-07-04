package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingProductDetailRepository extends JpaRepository<IncomingProductDetail, Integer> {
}
