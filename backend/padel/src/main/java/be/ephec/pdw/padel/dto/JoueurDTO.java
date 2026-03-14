package be.ephec.pdw.padel.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoueurDTO  {
    String matricule;
    String nom;
    String prenom;
}
