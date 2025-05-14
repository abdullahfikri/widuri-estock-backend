package dev.mfikri.widuriestock.model.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginRequest {
    @NotBlank
    @Size(max = 100, min = 5)
    private String username;

    @NotBlank
    @Size(max = 100, min = 8)
    private String password;
}
