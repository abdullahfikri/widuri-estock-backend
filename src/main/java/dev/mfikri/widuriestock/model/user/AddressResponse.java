package dev.mfikri.widuriestock.model.user;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private Integer id;
    private String street;
    private String village;
    private String district;
    private String city;
    private String province;
    private String country;
    private String postalCode;
    private String usernameId;
}
