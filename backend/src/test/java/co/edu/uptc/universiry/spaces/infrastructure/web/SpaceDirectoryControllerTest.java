package co.edu.uptc.universiry.spaces.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceDirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returns_the_sourced_location_directory_to_an_anonymous_visitor() throws Exception {
        mockMvc.perform(get("/api/v1/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations.length()").value(21))
                .andExpect(jsonPath("$.requestPathways.length()").value(5))
                .andExpect(jsonPath("$.requestPathways[0].id").value("auditoriums-admin-spaces"))
                .andExpect(jsonPath("$.locations[0].id").value("site-central-tunja"))
                .andExpect(jsonPath("$.locations[0].source.url").value(
                        "https://uptc.edu.co/sitio/portal/sitios/localizacion/"))
                .andExpect(jsonPath("$.officialOfficeDirectoryUrl").value(
                        "https://www.uptc.edu.co/sitio/portal/sitios/directorio/"));
    }

    @Test
    void denies_mutations_and_unpublished_subroutes() throws Exception {
        mockMvc.perform(post("/api/v1/spaces").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/spaces/cread-bogota").with(jwt()))
                .andExpect(status().isForbidden());
    }
}
