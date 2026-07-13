package be.ephec.pdw.padel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeAdminPasswordDTO {
    @NotBlank(message = "Mot de passe actuel requis")
    private String currentPassword;

    @NotBlank(message = "Nouveau mot de passe requis")
    @Size(min = 12, message = "Le mot de passe admin doit contenir au moins 12 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirmation requise")
    private String confirmPassword;
}
