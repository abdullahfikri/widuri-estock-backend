package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping(path = "/categories",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<CategoryResponse> create(@RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.create(request);

        return WebResponse.<CategoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<CategoryResponse> get(@PathVariable Integer categoryId) {
        CategoryResponse response = categoryService.get(categoryId);

        return WebResponse.<CategoryResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }
}
