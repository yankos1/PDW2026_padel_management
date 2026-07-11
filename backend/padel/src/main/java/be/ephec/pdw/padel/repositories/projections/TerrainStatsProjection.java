package be.ephec.pdw.padel.repositories.projections;

public interface TerrainStatsProjection {
    Long getTerrainId();
    String getTerrainNom();
    Long getSiteId();
    String getSiteNom();
    Long getNombreMatchs();
    Long getReservationsValides();
}
