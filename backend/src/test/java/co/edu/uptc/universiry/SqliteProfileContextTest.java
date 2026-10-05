package co.edu.uptc.universiry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code sqlite} profile exists so a backend run can be executed without a database container.
 * MySQL 8.4 remains the target engine; this test only proves the isolated profile boots.
 */
@SpringBootTest
@ActiveProfiles("sqlite")
class SqliteProfileContextTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void startsTheBackendOnAnInMemorySqliteDatabase() throws Exception {
        // Arrange + Act
        String url;
        try (var connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }

        // Assert
        assertThat(url).startsWith("jdbc:sqlite:");
        assertThat(new JdbcTemplate(dataSource).queryForObject("select 1", Integer.class)).isEqualTo(1);
    }
}