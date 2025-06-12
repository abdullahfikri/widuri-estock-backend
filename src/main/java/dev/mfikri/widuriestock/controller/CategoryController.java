package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;
import dev.mfikri.widuriestock.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public WebResponse<Category> create(@RequestBody CategoryCreateRequest request) {
        Category response = categoryService.create(request);

        return WebResponse.<Category>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/categories",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<Category>> getList() {
        List<Category> response = categoryService.getList();

        return WebResponse.<List<Category>>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<Category> get(@PathVariable Integer categoryId) {
        Category response = categoryService.get(categoryId);

        return WebResponse.<Category>builder()
                .data(response)
                .build();
    }

    @PutMapping(path = "/categories/{categoryId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<Category> update(@RequestBody CategoryUpdateRequest request, @PathVariable Integer categoryId) {
        request.setId(categoryId);
        Category response = categoryService.update(request);

        return WebResponse.<Category>builder()
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
