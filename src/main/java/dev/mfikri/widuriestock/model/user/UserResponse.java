package dev.mfikri.widuriestock.model.user;

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
public class UserResponse {
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String photo;
    private String role;
    private Set<AddressResponse> addresses;
}
