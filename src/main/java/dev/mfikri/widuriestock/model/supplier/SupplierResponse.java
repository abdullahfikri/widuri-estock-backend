package dev.mfikri.widuriestock.model.supplier;

import dev.mfikri.widuriestock.model.address.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierResponse {
    private Integer id;
    private String supplierName;
    private String phone;
    private String email;
    private String information;
    private Set<AddressResponse> addresses;
}
