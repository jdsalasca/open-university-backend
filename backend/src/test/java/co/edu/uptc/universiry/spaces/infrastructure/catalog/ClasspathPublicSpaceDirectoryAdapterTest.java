package co.edu.uptc.universiry.spaces.infrastructure.catalog;

import co.edu.uptc.universiry.spaces.application.PublicSpaceDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ClasspathPublicSpaceDirectoryAdapterTest {

    @Autowired
    private PublicSpaceDirectory directory;

    @Test
    void loads_the_versioned_directory_as_an_immutable_domain_snapshot() {
        var snapshot = directory.snapshot();

        assertNotNull(snapshot);
        assertEquals(24, snapshot.locations().size());
        assertEquals(5, snapshot.requestPathways().size());
        assertEquals("Sede Central Tunja", snapshot.locations().getFirst().name());
        var healthFaculty = snapshot.locations().stream()
                .filter(location -> "health-faculty-wellbeing-2026".equals(location.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("Espacios de bienestar e integración · Facultad de Ciencias de la Salud", healthFaculty.name());
        assertEquals("Tunja", healthFaculty.municipality());
        assertEquals("https://uptc.edu.co/sitio/portal/cal_not_eve/noticias/det/"
                        + "Facultad-de-Ciencias-de-la-Salud-de-la-UPTC-estrena-espacios-para-el-bienestar-e-integracion-de-sus-estudiantes/",
                healthFaculty.source().url());
        var music = snapshot.locations().stream()
                .filter(location -> "service-music-library-2026".equals(location.id()))
                .findFirst()
                .orElseThrow();
        assertNotNull(music.announcement());
        assertEquals(3, music.announcement().capacities().size());
        assertEquals("https://www.uptc.edu.co/sitio/portal/sitios/directorio/",
                snapshot.officialOfficeDirectoryUrl());
    }
}
