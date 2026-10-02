package co.edu.uptc.universiry.territories.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record TerritorialCatalogSnapshot(
        TerritorialCatalogSource source,
        List<TerritorialDepartment> departments,
        List<TerritorialEntity> entities
) {

    public TerritorialCatalogSnapshot {
        if (source == null || departments == null || departments.isEmpty()
                || entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Territorial catalog snapshot must include source, departments, and entities.");
        }
        if (departments.stream().anyMatch(Objects::isNull)
                || entities.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Territorial catalog snapshot cannot contain null rows.");
        }

        departments = departments.stream()
                .sorted((left, right) -> left.code().compareTo(right.code()))
                .toList();
        entities = entities.stream()
                .sorted((left, right) -> left.code().compareTo(right.code()))
                .toList();

        Map<String, TerritorialDepartment> departmentsByCode = new HashMap<>();
        for (TerritorialDepartment department : departments) {
            if (departmentsByCode.putIfAbsent(department.code(), department) != null) {
                throw new IllegalArgumentException("Territorial department codes must be unique and non-null.");
            }
        }

        Set<String> entityCodes = new HashSet<>();
        for (TerritorialEntity entity : entities) {
            if (!departmentsByCode.containsKey(entity.departmentCode())
                    || !entityCodes.add(entity.code())) {
                throw new IllegalArgumentException("Territorial entity codes must be unique and reference a known department.");
            }
        }
    }

    public TerritorialDepartment department(String code) {
        return departments.stream()
                .filter(department -> department.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    public List<TerritorialEntity> entitiesIn(String departmentCode) {
        return entities.stream()
                .filter(entity -> entity.departmentCode().equals(departmentCode))
                .toList();
    }
}
