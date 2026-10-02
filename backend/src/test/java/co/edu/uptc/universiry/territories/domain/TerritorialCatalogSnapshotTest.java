package co.edu.uptc.universiry.territories.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TerritorialCatalogSnapshotTest {

    private static final TerritorialCatalogSource SOURCE = new TerritorialCatalogSource(
            "DANE",
            "DIVIPOLA según Marco Geoestadístico Nacional",
            "MGN 2025",
            LocalDate.parse("2026-10-02"),
            "https://geoportal.dane.gov.co/mparcgis/rest/services/Divipola/Serv_DIVIPOLA_MGN_2025/FeatureServer",
            "https://www.dane.gov.co/index.php/sistema-estadistico-nacional-sen/normas-y-estandares/nomenclaturas-y-clasificaciones/nomenclaturas/codificacion-de-la-division-politica-administrativa-de-colombia-divipola"
    );

    @Test
    void rejects_null_departments_with_a_domain_validation_error() {
        var validEntity = new TerritorialEntity(
                "05001", "05", "001", "MEDELLÍN", TerritorialEntityType.MUNICIPIO, 2025);

        assertThrows(IllegalArgumentException.class,
                () -> new TerritorialCatalogSnapshot(SOURCE,
                        Arrays.asList(null, new TerritorialDepartment("05", "ANTIOQUIA")),
                        List.of(validEntity)));
    }

    @Test
    void rejects_null_entities_with_a_domain_validation_error() {
        var department = new TerritorialDepartment("05", "ANTIOQUIA");
        var validEntity = new TerritorialEntity(
                "05001", "05", "001", "MEDELLÍN", TerritorialEntityType.MUNICIPIO, 2025);

        assertThrows(IllegalArgumentException.class,
                () -> new TerritorialCatalogSnapshot(SOURCE, List.of(department),
                        Arrays.asList(null, validEntity)));
    }

    @Test
    void rejects_an_entity_that_references_an_unknown_department() {
        var knownDepartment = new TerritorialDepartment("05", "ANTIOQUIA");
        var unknownDepartmentEntity = new TerritorialEntity(
                "15001", "15", "001", "TUNJA", TerritorialEntityType.MUNICIPIO, 2025);

        assertThrows(IllegalArgumentException.class,
                () -> new TerritorialCatalogSnapshot(SOURCE, List.of(knownDepartment),
                        List.of(unknownDepartmentEntity)));
    }
}
