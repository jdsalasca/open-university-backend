package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:library-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LibraryControllerTest {

    private static final String LIBRARY = "/api/v1/admin/library";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityDirectory identities;

    /** Registers a synthetic university user through the real identity directory, as a first login would. */
    private String borrower() {
        return identities.registerIfAbsent(
                new AuthenticatedPrincipal("https://identity.example.edu", "library.reader"), Instant.EPOCH)
                .userId().toString();
    }

    private String secondBorrower() {
        return identities.registerIfAbsent(
                new AuthenticatedPrincipal("https://identity.example.edu", "library.other-reader"), Instant.EPOCH)
                .userId().toString();
    }

    private static String lendBody(String copyId, String borrowerUserId, String lentOn, String dueOn, String reference) {
        return """
                {"copyId":"%s","borrowerUserId":"%s","lentOn":"%s","dueOn":"%s","sourceReference":"%s"}"""
                .formatted(copyId, borrowerUserId, lentOn, dueOn, reference);
    }

    @Test
    void managing_the_library_requires_an_institutional_session() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(LIBRARY + "/titles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "visitor")
    void reading_the_catalogue_requires_the_library_read_permission() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(LIBRARY + "/titles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "visitor", authorities = "library:read")
    void registering_a_title_requires_the_library_write_permission() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post(LIBRARY + "/titles").contentType(MediaType.APPLICATION_JSON).content(titleBody()))
                .andExpect(status().isForbidden());
        assertEquals(0, countTitles());
    }

    @Test
    @WithLibraryPermissions
    void a_librarian_registers_a_title_with_its_authors_and_then_lends_and_returns_one_copy() throws Exception {
        // Arrange
        var title = mockMvc.perform(post(LIBRARY + "/titles")
                        .contentType(MediaType.APPLICATION_JSON).content(titleBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authors", hasSize(2)))
                .andReturn();
        String titleId = read(title.getResponse().getContentAsString(), "$.titleId");

        var copy = mockMvc.perform(post(LIBRARY + "/titles/" + titleId + "/copies")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"barcode":"BC-0001","location":"Estante A-3","sourceReference":"Acta 1 de 2026"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String copyId = read(copy.getResponse().getContentAsString(), "$.copyId");

        // Act: the lending unit decides the due date, so the platform stores the one it is given.
        var loan = mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(lendBody(copyId, borrower(), "2026-10-03", "2026-10-17", "Préstamo 1 de 2026")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueOn").value("2026-10-17"))
                .andReturn();
        String loanId = read(loan.getResponse().getContentAsString(), "$.loanId");

        // Assert
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM library_loan", Integer.class));
        mockMvc.perform(post(LIBRARY + "/loans/" + loanId + "/return").contentType(MediaType.APPLICATION_JSON).content("""
                {"returnedOn":"2026-10-09","sourceReference":"Devolución 1 de 2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedOn").value("2026-10-09"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_loan WHERE returned_on IS NULL", Integer.class));
    }

    @Test
    @WithLibraryPermissions
    void a_returned_loan_records_the_acting_actor_and_the_reference_separately() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);
        String loanId = registerLoan(copyId);

        // Act
        mockMvc.perform(post(LIBRARY + "/loans/" + loanId + "/return").contentType(MediaType.APPLICATION_JSON).content("""
                {"returnedOn":"2026-10-09","sourceReference":"Devolución 1 de 2026"}"""))
                .andExpect(status().isOk());

        // Assert: the closure trail separates who acted from the institutional reference.
        org.junit.jupiter.api.Assertions.assertEquals("librarian", jdbcTemplate.queryForObject(
                "SELECT closed_by FROM library_loan WHERE loan_id = ?", String.class, loanId));
        org.junit.jupiter.api.Assertions.assertEquals("Devolución 1 de 2026", jdbcTemplate.queryForObject(
                "SELECT closed_reference FROM library_loan WHERE loan_id = ?", String.class, loanId));
    }

    @Test
    @WithLibraryPermissions
    void a_new_loan_records_the_acting_librarian() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);

        // Act
        String loanId = registerLoan(copyId);

        // Assert: who opened the loan is auditable, not only who closed it.
        org.junit.jupiter.api.Assertions.assertEquals("librarian", jdbcTemplate.queryForObject(
                "SELECT created_by FROM library_loan WHERE loan_id = ?", String.class, loanId));
    }

    @Test
    @WithLibraryPermissions
    void a_copy_with_an_open_loan_cannot_be_lent_again() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);
        registerLoan(copyId);

        // Act + Assert
        mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(lendBody(copyId, secondBorrower(), "2026-10-04", "2026-10-18", "Préstamo 2 de 2026")))
                .andExpect(status().isConflict());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM library_loan", Integer.class));
    }

    @Test
    @WithLibraryPermissions
    void a_loan_cannot_be_returned_twice() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);
        String loanId = registerLoan(copyId);
        mockMvc.perform(post(LIBRARY + "/loans/" + loanId + "/return").contentType(MediaType.APPLICATION_JSON).content("""
                {"returnedOn":"2026-10-09","sourceReference":"Devolución 1 de 2026"}"""))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(post(LIBRARY + "/loans/" + loanId + "/return").contentType(MediaType.APPLICATION_JSON).content("""
                {"returnedOn":"2026-10-10","sourceReference":"Devolución repetida"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @WithLibraryPermissions
    void a_loan_requires_an_existing_canonical_university_user() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);

        // Act + Assert
        mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON).content("""
                {"copyId":"%s","borrowerUserId":"00000000-0000-4000-8000-000000000000",
                 "lentOn":"2026-10-03","dueOn":"2026-10-17","sourceReference":"Préstamo 3 de 2026"}""".formatted(copyId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithLibraryPermissions
    void withdrawing_a_copy_records_the_librarian_and_blocks_further_lending() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);

        // Act
        mockMvc.perform(post(LIBRARY + "/copies/" + copyId + "/withdraw").contentType(MediaType.APPLICATION_JSON).content("""
                {"sourceReference":"Resolución de descarte 7 de 2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Assert: leaving circulation is auditable and the withdrawn copy can no longer be lent.
        org.junit.jupiter.api.Assertions.assertEquals("librarian", jdbcTemplate.queryForObject(
                "SELECT withdrawn_by FROM library_copy WHERE copy_id = ?", String.class, copyId));
        org.junit.jupiter.api.Assertions.assertEquals("Resolución de descarte 7 de 2026", jdbcTemplate.queryForObject(
                "SELECT withdrawn_reference FROM library_copy WHERE copy_id = ?", String.class, copyId));
        mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(lendBody(copyId, borrower(), "2026-10-03", "2026-10-17", "Préstamo 9 de 2026")))
                .andExpect(status().isConflict());

        // The persisted trail is queryable through the API, not only in the database.
        mockMvc.perform(get(LIBRARY + "/titles/" + titleId + "/copies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].withdrawnBy").value("librarian"))
                .andExpect(jsonPath("$[0].withdrawnReference").value("Resolución de descarte 7 de 2026"))
                .andExpect(jsonPath("$[0].withdrawnAt").isNotEmpty());
    }

    @Test
    @WithLibraryPermissions
    void a_copy_cannot_be_withdrawn_twice() throws Exception {
        // Arrange
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);
        mockMvc.perform(post(LIBRARY + "/copies/" + copyId + "/withdraw").contentType(MediaType.APPLICATION_JSON).content("""
                {"sourceReference":"Resolución de descarte 7 de 2026"}"""))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(post(LIBRARY + "/copies/" + copyId + "/withdraw").contentType(MediaType.APPLICATION_JSON).content("""
                {"sourceReference":"Resolución de descarte 8 de 2026"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @WithLibraryPermissions
    void open_loans_lists_outstanding_copies_and_flags_the_overdue_ones() throws Exception {
        // Arrange: a loan whose due date is already behind the institutional clock.
        String titleId = registerTitle();
        String copyId = registerCopy(titleId);
        mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(lendBody(copyId, borrower(), "2020-01-01", "2020-02-01", "Préstamo 1 de 2026")))
                .andExpect(status().isCreated());
        String loanId = jdbcTemplate.queryForObject(
                "SELECT loan_id FROM library_loan WHERE returned_on IS NULL", String.class);

        // Act + Assert
        mockMvc.perform(get(LIBRARY + "/open-loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].loanId").value(loanId))
                .andExpect(jsonPath("$[0].overdue").value(true));

        // A returned copy is no longer outstanding.
        mockMvc.perform(post(LIBRARY + "/loans/" + loanId + "/return").contentType(MediaType.APPLICATION_JSON).content("""
                {"returnedOn":"2020-02-05","sourceReference":"Devolución 1 de 2026"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get(LIBRARY + "/open-loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithLibraryPermissions
    void searches_titles_without_letting_wildcards_leak_into_the_query() throws Exception {
        // Arrange: two titles, one of them with a different case and no accent overlap.
        mockMvc.perform(post(LIBRARY + "/titles").contentType(MediaType.APPLICATION_JSON).content(titleBody()))
                .andExpect(status().isCreated());
        mockMvc.perform(post(LIBRARY + "/titles").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Física universitaria","authors":["Autor tres"],"edition":"2a",
                 "publicationYear":2020,"sourceReference":"Acta 2 de 2026"}"""))
                .andExpect(status().isCreated());

        // Act + Assert: the search ignores case and a typed wildcard is a literal, not a pattern.
        mockMvc.perform(get(LIBRARY + "/titles").param("query", "LINEAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Álgebra lineal"));
        mockMvc.perform(get(LIBRARY + "/titles").param("query", "física"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(LIBRARY + "/titles").param("query", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String registerTitle() throws Exception {
        var created = mockMvc.perform(post(LIBRARY + "/titles")
                        .contentType(MediaType.APPLICATION_JSON).content(titleBody()))
                .andExpect(status().isCreated()).andReturn();
        return read(created.getResponse().getContentAsString(), "$.titleId");
    }

    private String registerCopy(String titleId) throws Exception {
        var created = mockMvc.perform(post(LIBRARY + "/titles/" + titleId + "/copies")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"barcode":"BC-0001","location":"Estante A-3","sourceReference":"Acta 1 de 2026"}"""))
                .andExpect(status().isCreated()).andReturn();
        return read(created.getResponse().getContentAsString(), "$.copyId");
    }

    private String registerLoan(String copyId) throws Exception {
        var created = mockMvc.perform(post(LIBRARY + "/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(lendBody(copyId, borrower(), "2026-10-03", "2026-10-17", "Préstamo 1 de 2026")))
                .andExpect(status().isCreated()).andReturn();
        return read(created.getResponse().getContentAsString(), "$.loanId");
    }

    private static String titleBody() {
        return """
                {"title":"Álgebra lineal","authors":["Autor uno","Autor dos"],"edition":"3a",
                 "publicationYear":2019,"sourceReference":"Acta 1 de 2026"}""";
    }

    private int countTitles() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM library_title", Integer.class);
    }

    private static String read(String body, String path) {
        return com.jayway.jsonpath.JsonPath.read(body, path);
    }

    private static void assertEquals(int expected, Integer actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}