package be.ephec.pdw.padel.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LIBRE")
public class MembreLibre extends Membre {}
