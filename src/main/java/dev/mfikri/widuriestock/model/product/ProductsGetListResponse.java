package dev.mfikri.widuriestock.model.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductsGetListResponse {
    private Integer id;
    private String name;
    private String description;
    private CategoryResponse categoryResponse;
    private String imageLocation;
}
