package co.edu.uptc.universiry.territories.domain;

public record TerritorialDepartment(String code, String name) {

    public TerritorialDepartment {
        if (code == null || !code.matches("[0-9]{2}")
                || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A territorial department requires a two-digit code and name.");
        }
    }
}
