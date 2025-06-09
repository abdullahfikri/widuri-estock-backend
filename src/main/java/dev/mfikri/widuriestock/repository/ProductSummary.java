package dev.mfikri.widuriestock.repository;


import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.entity.product.ProductPhoto;

public interface ProductSummary {
    Integer getId();
    String getName();
    String getDescription();
    Category getCategory();
    ProductPhoto getProductPhotos();
}
