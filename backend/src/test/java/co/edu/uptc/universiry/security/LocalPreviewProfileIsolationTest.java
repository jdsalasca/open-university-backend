package co.edu.uptc.universiry.security;

import co.edu.uptc.universiry.roomplanning.infrastructure.web.RoomAllocationProposalController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:local-preview-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalPreviewProfileIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void local_preview_session_issuer_is_not_available_outside_its_profile() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post("/api/v1/dev/local-preview-session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void room_allocation_preview_controller_is_not_loaded_outside_its_profile() {
        // Arrange + Act
        var matchingBeans = applicationContext.getBeansOfType(RoomAllocationProposalController.class);

        // Assert
        assertTrue(matchingBeans.isEmpty());
    }
}
