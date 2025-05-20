package dev.mfikri.widuriestock.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class UserCurrrentUpdateRequest {
    @JsonIgnore
    @NotBlank(message = "Username must not blank")
    private String username;

    @Size(min = 8, max = 100)
    private String password;

    private String firstName;

    @Size(max = 100)
    private String lastName;

    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    private MultipartFile photo;

}
