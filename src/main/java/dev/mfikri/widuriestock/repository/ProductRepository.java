package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsByName(String name);

    @Query(
            value = "SELECT " +
                    "    p.id as id, " +
                    "    p.name as name, " +
                    "    p.description as description, " +
                    "    c.id as categoryId, " +
                    "    c.name as categoryName, " +
                    "    (SELECT pp.image_location FROM product_photos pp WHERE pp.product_id = p.id ORDER BY pp.id ASC LIMIT 1) as imageLocation " +
                    "FROM products p " +
                    "JOIN (SELECT id FROM products ORDER BY name LIMIT :#{#pageable.offset}, :#{#pageable.pageSize}) AS p_page ON p.id = p_page.id " +
                    "JOIN categories c ON p.category_id = c.id " +
                    "ORDER BY p.name ASC",
            countQuery = "SELECT count(*) FROM products p",
            nativeQuery = true

    )
    Page<ProductSummary> findProductListView(Pageable pageable);
}

