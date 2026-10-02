package co.edu.uptc.universiry.roomplanning.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:room-allocation-preview;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles({"test", "local-preview"})
class RoomAllocationPreviewControllerTest {

    private static final String SESSION_ENDPOINT = "/api/v1/dev/local-preview-session";
    private static final String PROPOSAL_ENDPOINT = "/api/v1/dev/room-allocation/proposals";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticated_preview_returns_a_deterministic_suggestion_without_creating_a_session_cookie() throws Exception {
        // Arrange
        String token = localPreviewToken();
        String request = """
                {
                  "groups": [
                    {"code":"DEMO-A","expectedEnrollment":22,"requiredFeatures":["COMPUTER_STATIONS"],"meetings":[{"day":"TUESDAY","startsAt":"08:00","endsAt":"10:00"}]},
                    {"code":"DEMO-B","expectedEnrollment":18,"requiredFeatures":[],"meetings":[{"day":"TUESDAY","startsAt":"08:00","endsAt":"10:00"}]}
                  ],
                  "rooms": [
                    {"code":"DEMO-20","capacity":20,"active":true,"features":[]},
                    {"code":"DEMO-LAB-24","capacity":24,"active":true,"features":["COMPUTER_STATIONS"]},
                    {"code":"DEMO-40","capacity":40,"active":true,"features":["COMPUTER_STATIONS"]}
                  ]
                }
                """;

        // Act + Assert
        var result = mockMvc.perform(post(PROPOSAL_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.assignedGroups").value(2))
                .andExpect(jsonPath("$.unassignedGroups").value(0))
                .andExpect(jsonPath("$.unusedSeats").value(4))
                .andExpect(jsonPath("$.placements[0].groupCode").value("DEMO-A"))
                .andExpect(jsonPath("$.placements[0].roomCode").value("DEMO-LAB-24"))
                .andExpect(jsonPath("$.placements[1].groupCode").value("DEMO-B"))
                .andExpect(jsonPath("$.placements[1].roomCode").value("DEMO-20"))
                .andReturn();

        var repeated = mockMvc.perform(post(PROPOSAL_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn();
        Object firstPlacements = JsonPath.read(result.getResponse().getContentAsString(), "$.placements");
        Object repeatedPlacements = JsonPath.read(repeated.getResponse().getContentAsString(), "$.placements");
        assertEquals(firstPlacements, repeatedPlacements);
    }

    @Test
    void preview_requires_authentication() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post(PROPOSAL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejects_duplicate_codes_and_invalid_meeting_intervals() throws Exception {
        // Arrange
        String token = localPreviewToken();
        String invalid = """
                {
                  "groups": [
                    {"code":"DEMO-A","expectedEnrollment":10,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"09:00","endsAt":"09:00"}]},
                    {"code":"DEMO-A","expectedEnrollment":10,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"10:00","endsAt":"11:00"}]}
                  ],
                  "rooms": [{"code":"DEMO-1","capacity":20,"active":true,"features":[]}]
                }
                """;

        // Act + Assert
        mockMvc.perform(post(PROPOSAL_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_room_allocation"))
                .andExpect(jsonPath("$.message").value(containsString("inválid")));
    }

    @Test
    void rejects_requests_larger_than_the_scenario_limit() throws Exception {
        // Arrange
        String token = localPreviewToken();
        String tooManyGroups = """
                {
                  "groups": [
                    {"code":"G01","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G02","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G03","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G04","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G05","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G06","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G07","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G08","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G09","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G10","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G11","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G12","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]},
                    {"code":"G13","expectedEnrollment":1,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]}
                  ],
                  "rooms": [{"code":"DEMO-1","capacity":1,"active":true,"features":[]}]
                }
                """;

        // Act + Assert
        mockMvc.perform(post(PROPOSAL_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tooManyGroups))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_room_allocation"));
    }

    private String localPreviewToken() throws Exception {
        String response = mockMvc.perform(post(SESSION_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String validRequest() {
        return """
                {"groups":[{"code":"DEMO-A","expectedEnrollment":10,"requiredFeatures":[],"meetings":[{"day":"MONDAY","startsAt":"08:00","endsAt":"09:00"}]}],
                 "rooms":[{"code":"DEMO-1","capacity":20,"active":true,"features":[]}]}
                """;
    }
}
