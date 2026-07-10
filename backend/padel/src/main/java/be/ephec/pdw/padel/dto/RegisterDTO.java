package be.ephec.pdw.padel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank
    @Size(max = 80)
    private String nom;

    @NotBlank
    @Size(max = 80)
    private String prenom;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;
}
