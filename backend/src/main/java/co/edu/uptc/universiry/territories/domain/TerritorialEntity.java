package co.edu.uptc.universiry.territories.domain;

public record TerritorialEntity(
        String code,
        String departmentCode,
        String localCode,
        String name,
        TerritorialEntityType type,
        int dataYear
) {

    public TerritorialEntity {
        if (code == null || !code.matches("[0-9]{5}")
                || departmentCode == null || !departmentCode.matches("[0-9]{2}")
                || localCode == null || !localCode.matches("[0-9]{3}")
                || !code.equals(departmentCode + localCode)
                || name == null || name.isBlank()
                || type == null
                || dataYear < 1900 || dataYear > 2100) {
            throw new IllegalArgumentException("Territorial entity has an invalid DIVIPOLA code or source value.");
        }
    }
}
