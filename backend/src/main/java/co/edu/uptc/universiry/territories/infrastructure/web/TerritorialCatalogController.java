package co.edu.uptc.universiry.territories.infrastructure.web;

import co.edu.uptc.universiry.territories.application.TerritorialCatalog;
import co.edu.uptc.universiry.territories.domain.TerritorialCatalogSnapshot;
import co.edu.uptc.universiry.territories.domain.TerritorialCatalogSource;
import co.edu.uptc.universiry.territories.domain.TerritorialDepartment;
import co.edu.uptc.universiry.territories.domain.TerritorialEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public final class TerritorialCatalogController {

    private final TerritorialCatalog catalog;

    public TerritorialCatalogController(TerritorialCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/v1/territorial-catalog/departments")
    public DepartmentsResponse departments() {
        TerritorialCatalogSnapshot snapshot = catalog.snapshot();
        return new DepartmentsResponse(snapshot.source(), snapshot.departments());
    }

    @GetMapping("/api/v1/territorial-catalog/departments/{departmentCode}/entities")
    public DepartmentEntitiesResponse entities(@PathVariable String departmentCode) {
        if (!departmentCode.matches("[0-9]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department code must contain two digits.");
        }

        TerritorialCatalogSnapshot snapshot = catalog.snapshot();
        TerritorialDepartment department = snapshot.department(departmentCode);
        if (department == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department code was not found.");
        }

        return new DepartmentEntitiesResponse(
                snapshot.source(), department, snapshot.entitiesIn(departmentCode));
    }

    public record DepartmentsResponse(TerritorialCatalogSource source, List<TerritorialDepartment> departments) {
    }

    public record DepartmentEntitiesResponse(
            TerritorialCatalogSource source,
            TerritorialDepartment department,
            List<TerritorialEntity> entities
    ) {
    }
}
