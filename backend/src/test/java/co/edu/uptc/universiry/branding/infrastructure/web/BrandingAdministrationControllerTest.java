package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.security.WithBrandingPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrandingAdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymous_update_returns_json_401() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("Se requiere autenticación."))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    @WithBrandingPermissions
    void brand_admin_can_publish_a_new_branding_revision() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.institutionName").value("Universidad UPTC"))
                .andExpect(jsonPath("$.colors.primary").value("#E0C037"));
    }

    @Test
    @WithMockUser(username = "brand.viewer", authorities = "branding:read")
    void branding_reader_can_view_administrative_configuration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    @WithMockUser(username = "brand.viewer", authorities = "branding:read")
    void branding_reader_cannot_publish_a_revision() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithMockUser(username = "staff", authorities = "USER")
    void rejects_authenticated_user_without_brand_admin_authority() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .header("Accept-Language", "en-US")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("The authenticated user lacks the required permission."));
    }

    @Test
    @WithBrandingPermissions
    void brand_admin_cannot_access_unregistered_administration_routes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithBrandingPermissions
    void brand_admin_cannot_access_unregistered_branding_subroutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/branding/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithBrandingPermissions
    void brand_admin_cannot_use_unregistered_methods_on_branding_routes() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/branding"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithBrandingPermissions
    void stale_revision_returns_conflict_without_overwriting_current_configuration() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1).replace("Universidad UPTC", "Cambio obsoleto")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("revision_conflict"))
                .andExpect(jsonPath("$.message").value("La configuración de identidad cambió en otra sesión. Recárgala antes de publicar."));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionName").value("Universidad UPTC"));
    }

    @Test
    @WithBrandingPermissions
    void rollback_publishes_the_historical_snapshot_as_a_new_revision() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1)))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/admin/branding/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRevision\":1,\"expectedRevision\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(3))
                .andExpect(jsonPath("$.institutionName").value("Universidad Pedagógica y Tecnológica de Colombia"))
                .andExpect(jsonPath("$.colors.primary").value("#FFCC29"));
    }

    @Test
    @WithBrandingPermissions
    void invalid_color_and_blank_module_label_return_safe_validation_error() throws Exception {
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1).replace("#E0C037", "url(javascript:alert(1))")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));

        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validChange(1).replace("\"Estudiantes\"", "\"   \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    private static String validChange(long expectedRevision) {
        return """
                {
                  "expectedRevision": %d,
                  "institutionName": "Universidad UPTC",
                  "colors": {
                    "primary": "#E0C037", "ink": "#1A1A1A", "surface": "#FFFFFF",
                    "text": "#1A1A1A", "accent": "#FFCC29", "focus": "#1A1A1A"
                  },
                  "assets": {"logoLight": null, "logoDark": null, "favicon": null},
                  "modules": [
                    {"key":"home","label":"Inicio","available":true,"visible":true,"order":10},
                    {"key":"students","label":"Estudiantes","available":false,"visible":false,"order":20},
                    {"key":"programs","label":"Programas","available":false,"visible":false,"order":30},
                    {"key":"curricula","label":"Mallas curriculares","available":false,"visible":false,"order":40},
                    {"key":"subjects","label":"Asignaturas","available":false,"visible":false,"order":50},
                    {"key":"academic-load","label":"Carga académica","available":false,"visible":false,"order":60},
                    {"key":"visual-identity","label":"Identidad visual","available":true,"visible":true,"order":90},
                    {"key":"admissions","label":"Admisiones","available":true,"visible":true,"order":100}
                  ],
                  "banners": []
                }
                """.formatted(expectedRevision);
    }
}
