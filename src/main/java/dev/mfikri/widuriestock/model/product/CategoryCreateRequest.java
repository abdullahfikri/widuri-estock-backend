package dev.mfikri.widuriestock.model.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryCreateRequest {
    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    @Size(min = 3, max = 255)
    private String description;
}
