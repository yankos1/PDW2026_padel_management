package be.ephec.pdw.padel.dto;

import java.math.BigDecimal;

public record RevenuePeriodDto(
        int annee,
        int mois,
        BigDecimal montant
) {
}
