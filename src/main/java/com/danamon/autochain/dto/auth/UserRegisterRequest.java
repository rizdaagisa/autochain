package com.danamon.autochain.dto.auth;

import com.danamon.autochain.entity.UserRole;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 4, max = 126, message = "must be greater than 6 character and less than 126 character")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 4, message = "must be greater than 6 character")
    private String password;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    @Size(min = 4, message = "must be greater than 6 character")
    private String email;

    @Size(max = 128, message = "must be less than 128 character")
    private String company_id;

    @Size(max = 128, message = "must be less than 128 character")
    private List<String> role_id;

}
