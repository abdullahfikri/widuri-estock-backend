package dev.mfikri.widuriestock.model.incoming_product;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomingProductGetListRequest {

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer page;
    private Integer size;

}
