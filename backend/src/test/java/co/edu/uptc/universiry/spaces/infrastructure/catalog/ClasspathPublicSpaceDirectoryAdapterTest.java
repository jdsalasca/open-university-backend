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
        assertEquals(21, snapshot.locations().size());
        assertEquals(5, snapshot.requestPathways().size());
        assertEquals("Sede Central Tunja", snapshot.locations().getFirst().name());
        assertEquals("https://www.uptc.edu.co/sitio/portal/sitios/directorio/",
                snapshot.officialOfficeDirectoryUrl());
    }
}
