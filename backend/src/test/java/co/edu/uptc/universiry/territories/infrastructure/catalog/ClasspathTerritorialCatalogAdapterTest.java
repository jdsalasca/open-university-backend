package co.edu.uptc.universiry.territories.infrastructure.catalog;

import co.edu.uptc.universiry.territories.application.TerritorialCatalog;
import co.edu.uptc.universiry.territories.domain.TerritorialEntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ClasspathTerritorialCatalogAdapterTest {

    @Autowired
    private TerritorialCatalog catalog;

    @Test
    void loads_the_attributed_mgn_2025_snapshot_with_distinct_territorial_types() {
        var snapshot = catalog.snapshot();

        assertNotNull(snapshot);
        assertEquals("DANE", snapshot.source().publisher());
        assertEquals("MGN 2025", snapshot.source().datasetVersion());
        assertEquals(33, snapshot.departments().size());
        assertEquals(1122, snapshot.entities().size());
        assertEquals(1103, snapshot.entities().stream()
                .filter(entity -> entity.type() == TerritorialEntityType.MUNICIPIO).count());
        assertEquals(1, snapshot.entities().stream()
                .filter(entity -> entity.type() == TerritorialEntityType.ISLA).count());
        assertEquals(18, snapshot.entities().stream()
                .filter(entity -> entity.type() == TerritorialEntityType.AREA_NO_MUNICIPALIZADA).count());
        assertEquals("05", snapshot.departments().getFirst().code());
        assertEquals("05001", snapshot.entities().getFirst().code());
    }
}
