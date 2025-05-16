package dev.mfikri.widuriestock.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressModel {
    private Integer id;
    private String street;
    private String village;
    private String district;
    private String city;
    private String province;
    private String country;
    private String postalCode;
}
