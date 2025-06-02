package dev.mfikri.widuriestock.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressCreateRequest {
    @JsonIgnore
    @Null
    private Integer id;

    @JsonIgnore
    @Size(max = 100, min = 5)
    private String username;

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

    @NotBlank
    @Size(max = 100, min = 1)
    private String postalCode;
}
