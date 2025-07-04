package dev.mfikri.widuriestock.model.incoming_product;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductGetListRequest {
    private LocalDate startDate;
    private LocalDate endDate;

    @NotNull
    private Integer page;
    @NotNull
    private Integer size;
}
