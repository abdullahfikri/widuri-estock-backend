package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;
import dev.mfikri.widuriestock.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final ValidationService validationService;
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(ValidationService validationService, CategoryRepository categoryRepository) {
        this.validationService = validationService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Category create(CategoryCreateRequest request) {
        validationService.validate(request);

        boolean isExists = categoryRepository.existsByName(request.getName());
        if (isExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is already exists.");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);

        return category;
    }

    @Override
    public Category get(Integer id) {
        return this.findCategoryByIdOrThrows(id);
    }

    @Override
    public List<Category> getList() {
        return categoryRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    @Override
    public Category update(CategoryUpdateRequest request) {
        validationService.validate(request);

        Category category = this.findCategoryByIdOrThrows(request.getId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);

        return category;
    }

    @Override
    public void delete(Integer id) {
        Category category = findCategoryByIdOrThrows(id);

        categoryRepository.deleteById(category.getId());
    }

    private Category findCategoryByIdOrThrows(Integer id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category is not found."));
    }

//    private CategoryResponse toCategoryResponse(Category category) {
//        return CategoryResponse.builder()
//                .id(category.getId())
//                .name(category.getName())
//                .description(category.getDescription())
//                .build();
//    }
}
