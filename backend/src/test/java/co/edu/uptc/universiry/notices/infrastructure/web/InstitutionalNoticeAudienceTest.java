package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService;
import co.edu.uptc.universiry.notices.application.PublishInstitutionalNoticeCommand;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;
import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:institutional-notice-audience;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstitutionalNoticeAudienceTest {

    private static final String MY_NOTICES = "/api/v1/notices";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Autowired
    private InstitutionalNoticeService service;

    @Test
    void reading_my_notices_requires_an_institutional_session() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(MY_NOTICES))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void a_caller_without_scopes_only_sees_university_wide_notices() throws Exception {
        // Arrange
        service.publish(command("Aviso para toda la universidad", NoticeAudienceKind.UNIVERSITY, null), "editor");
        service.publish(command("Aviso de un programa", NoticeAudienceKind.PROGRAM, "ING-SIS"), "editor");

        // Act + Assert
        mockMvc.perform(get(MY_NOTICES).with(readerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices", hasSize(1)))
                .andExpect(jsonPath("$.notices[0].title").value("Aviso para toda la universidad"));
    }

    @Test
    void a_notice_outside_its_availability_window_is_not_visible() throws Exception {
        // Arrange
        LocalDate today = LocalDate.now(clock);
        service.publish(new PublishInstitutionalNoticeCommand(
                "Aviso de un periodo futuro", "Cuerpo", "Resolución 1 de 2026",
                today.plusDays(5), today.plusDays(9),
                List.of(new NoticeAudience(NoticeAudienceKind.UNIVERSITY, null))), "editor");

        // Act + Assert
        mockMvc.perform(get(MY_NOTICES).with(readerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices", hasSize(0)));
    }

    @Test
    void my_notices_never_expose_the_publishing_actor() throws Exception {
        // Arrange
        service.publish(command("Aviso para toda la universidad", NoticeAudienceKind.UNIVERSITY, null), "notice.editor");

        // Act
        var response = mockMvc.perform(get(MY_NOTICES).with(readerSession())).andExpect(status().isOk()).andReturn();

        // Assert
        assertFalse(response.getResponse().getContentAsString().contains("notice.editor"),
                "my notices describe content, not who published them");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor readerSession() {
        return jwt().jwt(token -> token.issuer("https://identity.example.edu").subject("reader"));
    }

    private PublishInstitutionalNoticeCommand command(String title, NoticeAudienceKind kind, String reference) {
        LocalDate today = LocalDate.now(clock);
        return new PublishInstitutionalNoticeCommand(
                title, "Cuerpo del aviso", "Resolución 1 de 2026",
                today, today.plusDays(7), List.of(new NoticeAudience(kind, reference)));
    }
}
