package dev.mfikri.widuriestock.model.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mfikri.widuriestock.model.address.AddressCreateRequest;
import dev.mfikri.widuriestock.model.address.AddressUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierUpdateRequest {

    @NotNull
    @JsonIgnore
    private Integer supplierId;

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
    private AddressSupplierUpdateRequest address;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AddressSupplierUpdateRequest {
        @NotBlank
        @Size(max = 100, min = 1)
        private String street;

        @Size(max = 100, min = 1)
        private String village;

        @Size(max = 100, min = 1)
        private String district;

        @NotBlank
        @Size(max = 100, min = 1)
        private String city;

        @NotBlank
        @Size(max = 100, min = 1)
        private String province;

        @NotBlank
        @Size(max = 100, min = 1)
        private String country;

        @Size(max = 10, min = 1)
        private String postalCode;
    }


}
