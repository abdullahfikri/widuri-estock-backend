package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductVariantDetail;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingProductVariantDetailRepository extends CrudRepository<IncomingProductVariantDetail, Integer> {
}
