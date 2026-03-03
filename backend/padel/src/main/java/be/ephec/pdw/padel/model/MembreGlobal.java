package be.ephec.pdw.padel.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("GLOBAL")
public class MembreGlobal extends Membre {}
