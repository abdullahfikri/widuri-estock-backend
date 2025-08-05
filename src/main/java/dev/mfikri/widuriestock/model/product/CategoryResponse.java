package dev.mfikri.widuriestock.model.product;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    private Integer id;
    private String name;
    private String description;
}
