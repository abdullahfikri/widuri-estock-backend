package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Category;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {
    Category create(CategoryCreateRequest request);
    Category get(Integer id);
    List<Category> getList();
    Category update(CategoryUpdateRequest request);
    void delete(Integer id);
}
