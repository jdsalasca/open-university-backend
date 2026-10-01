package co.edu.uptc.universiry.branding.domain;

import java.util.List;

public record BrandModule(String key, String label, boolean available, boolean visible, int displayOrder) {

    public static List<BrandModule> defaultCatalog() {
        return List.of(
                new BrandModule("home", "Inicio", true, true, 10),
                new BrandModule("students", "Estudiantes", false, false, 20),
                new BrandModule("programs", "Programas", false, false, 30),
                new BrandModule("curricula", "Mallas curriculares", false, false, 40),
                new BrandModule("subjects", "Asignaturas", false, false, 50),
                new BrandModule("academic-load", "Carga académica", false, false, 60),
                new BrandModule("visual-identity", "Identidad visual", true, true, 90),
                new BrandModule("admissions", "Admisiones", true, true, 100)
        );
    }
}
