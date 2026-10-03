package co.edu.uptc.universiry.security.localpreview;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:open-api-docs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles({"test", "local-preview"})
@Transactional
class OpenApiDocumentationTest {

    private static final String SESSION_ENDPOINT = "/api/v1/dev/local-preview-session";
    private static final String API_DOCS_ENDPOINT = "/v3/api-docs";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void api_documentation_description_is_not_public() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(API_DOCS_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void api_documentation_describes_the_public_and_administrative_surface_for_a_local_preview_session() throws Exception {
        // Arrange
        String token = localPreviewToken();

        // Act
        var response = mockMvc.perform(get(API_DOCS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", containsString("3.")))
                .andExpect(jsonPath("$.info.title", containsString("Universiry")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/admissions/calls")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/spaces")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/admin/admissions/calls")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/admin/academic-structure/audit-events")))
                .andReturn();

        // Assert: the description must not disclose the developer preview session as an API of the product.
        var document = JsonPath.read(response.getResponse().getContentAsString(), "$");
        org.junit.jupiter.api.Assertions.assertFalse(
                document.toString().contains("/api/v1/dev/local-preview-session"),
                "the local preview session issuer stays out of the documented API surface");
    }

    private String localPreviewToken() throws Exception {
        var issued = mockMvc.perform(post(SESSION_ENDPOINT)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(issued.getResponse().getContentAsString(), "$.accessToken");
    }
}