package be.ephec.pdw.padel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank
    @Pattern(regexp = "^[GSLgsl][0-9]{4}$", message = "Matricule invalide")
    private String matricule;

    private String password;
}
