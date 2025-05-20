package dev.mfikri.widuriestock.model.user;

import dev.mfikri.widuriestock.constraint.CheckRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCreateRequest {
    @NotBlank
    @Size(min = 5, max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    private String firstName;
    private String lastName;

    @NotBlank
    private String phone;

    @Email
    private String email;

    private MultipartFile photo;

    @NotBlank
    @CheckRole
    private String role;

    private AddressResponse address;

}
