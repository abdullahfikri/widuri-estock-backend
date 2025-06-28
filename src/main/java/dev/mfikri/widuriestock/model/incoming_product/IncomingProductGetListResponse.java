package dev.mfikri.widuriestock.model.incoming_product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductGetListResponse {
    private Integer id;
    private LocalDate dateIn;
    private IncomingProductSupplierResponse supplier;
    private String username;
    private Integer totalProducts;
    private String note;
}
