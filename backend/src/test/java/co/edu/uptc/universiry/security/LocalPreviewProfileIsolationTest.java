package co.edu.uptc.universiry.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:local-preview-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalPreviewProfileIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void local_preview_session_issuer_is_not_available_outside_its_profile() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post("/api/v1/dev/local-preview-session"))
                .andExpect(status().isUnauthorized());
    }
}
