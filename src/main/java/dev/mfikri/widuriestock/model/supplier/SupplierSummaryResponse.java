package dev.mfikri.widuriestock.model.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierSummaryResponse {
    private Integer id;
    private String supplierName;
    private String phone;
    private String email;
    private String information;
}
