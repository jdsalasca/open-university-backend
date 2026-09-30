package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicDisplayOrderCommand;
import co.edu.uptc.universiry.academics.application.AcademicStructureConflictException;
import co.edu.uptc.universiry.academics.application.AcademicStructureRepository;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-structure-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
class AcademicStructureConcurrencyTest {

    @Autowired
    private AcademicStructureRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrent_order_changes_from_the_same_value_allow_one_change_and_one_audit_event() throws Exception {
        // Arrange
        AcademicOrganizationUnit unit = createUnit();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Act
        try {
            Future<Boolean> first = executor.submit(() -> changeAfter(start, unit.id(), 1, "structure.operator.one"));
            Future<Boolean> second = executor.submit(() -> changeAfter(start, unit.id(), 2, "structure.operator.two"));
            start.countDown();

            // Assert
            assertTrue(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS));
            Integer storedOrder = jdbcTemplate.queryForObject("""
                    SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?
                    """, Integer.class, unit.id().toString());
            assertTrue(storedOrder == 1 || storedOrder == 2);
            assertEquals(1, countOrderEvents(unit.id()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void audit_insert_failure_rolls_back_the_order_change() {
        // Arrange
        AcademicOrganizationUnit unit = createUnit();
        jdbcTemplate.execute("ALTER TABLE academic_structure_audit_event "
                + "ADD CONSTRAINT ck_test_reject_unit_order CHECK (action_key <> 'UNIT_ORDER_CHANGED')");

        // Act
        try {
            assertThrows(DataIntegrityViolationException.class, () -> repository.changeOrganizationUnitOrder(
                    unit.id(), new AcademicDisplayOrderCommand(0, 7, "Synthetic rollback verification"),
                    "structure.operator"));
        } finally {
            jdbcTemplate.execute("ALTER TABLE academic_structure_audit_event DROP CONSTRAINT ck_test_reject_unit_order");
        }

        // Assert
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?
                """, Integer.class, unit.id().toString()));
        assertEquals(0, countOrderEvents(unit.id()));
    }

    private boolean changeAfter(CountDownLatch start, UUID unitId, int targetOrder, String actor) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start timed out.");
        try {
            repository.changeOrganizationUnitOrder(unitId,
                    new AcademicDisplayOrderCommand(0, targetOrder, "Synthetic concurrent verification"), actor);
            return true;
        } catch (AcademicStructureConflictException conflict) {
            return false;
        }
    }

    private AcademicOrganizationUnit createUnit() {
        UUID id = UUID.randomUUID();
        AcademicOrganizationUnit unit = new AcademicOrganizationUnit(id,
                "CONCURRENT-" + id.toString().substring(0, 8), AcademicOrganizationUnitType.FACULTY,
                "Unidad sintética de prueba", 0, LocalDate.now(ZoneId.of("America/Bogota")), null);
        repository.createOrganizationUnit(unit, "structure.setup", "Synthetic integration-test fixture");
        return unit;
    }

    private int countOrderEvents(UUID unitId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'UNIT_ORDER_CHANGED'
                """, Integer.class, unitId.toString());
    }
}
