package dev.mfikri.widuriestock.model.supplier;

import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierCreateRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String supplierName;

    @NotBlank
    @Size(min = 5, max = 20)
    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 255)
    private String information;

    @NotNull
    @Valid
    private AddressCreateRequest address;

}
