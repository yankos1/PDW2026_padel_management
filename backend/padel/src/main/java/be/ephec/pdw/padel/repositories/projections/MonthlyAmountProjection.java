package be.ephec.pdw.padel.repositories.projections;

public interface MonthlyAmountProjection {
    Integer getAnnee();
    Integer getMois();
    Double getMontant();
}
