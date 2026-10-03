package co.edu.uptc.universiry.library.infrastructure.persistence;

import co.edu.uptc.universiry.library.application.LibraryRepository;
import co.edu.uptc.universiry.library.domain.LibraryCopy;
import co.edu.uptc.universiry.library.domain.LibraryTitle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcLibraryRepositoryAdapter implements LibraryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcLibraryRepositoryAdapter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void appendTitle(LibraryTitle title, String actorSub) {
        jdbcTemplate.update("""
                INSERT INTO library_title
                    (title_id, title, edition, publication_year, source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                title.titleId(), title.title(), title.edition(), title.publicationYear(),
                title.sourceReference(), actorSub, Timestamp.from(clock.instant()));

        for (int order = 0; order < title.authors().size(); order++) {
            jdbcTemplate.update("""
                    INSERT INTO library_title_author (title_id, author_order, author_name)
                    VALUES (?, ?, ?)
                    """, title.titleId(), order, title.authors().get(order));
        }
    }

    @Override
    public void appendCopy(LibraryCopy copy, String actorSub) {
        jdbcTemplate.update("""
                INSERT INTO library_copy
                    (copy_id, title_id, barcode, location, active, source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                copy.copyId(), copy.titleId(), copy.barcode(), copy.location(), copy.active(),
                copy.sourceReference(), actorSub, Timestamp.from(clock.instant()));
    }

    @Override
    public List<LibraryTitle> titles(int limit) {
        Map<String, TitleAccumulator> titles = new LinkedHashMap<>();
        RowCallbackHandler collector = rs -> {
            String titleId = rs.getString("title_id");
            TitleAccumulator title = titles.get(titleId);
            if (title == null) {
                title = new TitleAccumulator(rs);
                titles.put(titleId, title);
            }
            String author = rs.getString("author_name");
            if (author != null) {
                title.authors.add(author);
            }
        };
        jdbcTemplate.query("""
                SELECT t.title_id, t.title, t.edition, t.publication_year, t.source_reference, a.author_name
                FROM (
                    SELECT title_id, title, edition, publication_year, source_reference
                    FROM library_title
                    ORDER BY title, title_id
                    LIMIT ?
                ) t
                LEFT JOIN library_title_author a ON a.title_id = t.title_id
                ORDER BY t.title, t.title_id, a.author_order
                """, collector, limit);
        return titles.values().stream().map(TitleAccumulator::toTitle).toList();
    }

    @Override
    public List<LibraryCopy> copiesOf(String titleId) {
        return jdbcTemplate.query("""
                SELECT copy_id, title_id, barcode, location, active, source_reference
                FROM library_copy
                WHERE title_id = ?
                ORDER BY barcode
                """, (rs, index) -> copy(rs), titleId);
    }

    @Override
    public Optional<LibraryCopy> lockCopy(String copyId) {
        return jdbcTemplate.query("""
                SELECT copy_id, title_id, barcode, location, active, source_reference
                FROM library_copy
                WHERE copy_id = ?
                FOR UPDATE
                """, rs -> rs.next() ? Optional.of(copy(rs)) : Optional.<LibraryCopy>empty(), copyId);
    }

    @Override
    public Optional<LibraryCopy.Loan> openLoanOf(String copyId) {
        return jdbcTemplate.query("""
                SELECT loan_id, copy_id, borrower_user_id, lent_on, due_on, returned_on, source_reference
                FROM library_loan
                WHERE copy_id = ? AND returned_on IS NULL
                ORDER BY created_at
                LIMIT 1
                """, rs -> rs.next() ? Optional.of(loan(rs)) : Optional.<LibraryCopy.Loan>empty(), copyId);
    }

    @Override
    public Optional<LibraryCopy.Loan> findLoan(String loanId) {
        return jdbcTemplate.query("""
                SELECT loan_id, copy_id, borrower_user_id, lent_on, due_on, returned_on, source_reference
                FROM library_loan
                WHERE loan_id = ?
                """, rs -> rs.next() ? Optional.of(loan(rs)) : Optional.<LibraryCopy.Loan>empty(), loanId);
    }

    @Override
    public void appendLoan(LibraryCopy.Loan loan, String actorSub) {
        jdbcTemplate.update("""
                INSERT INTO library_loan
                    (loan_id, copy_id, borrower_user_id, lent_on, due_on, returned_on, source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?)
                """,
                loan.loanId(), loan.copyId(), loan.borrowerUserId(), Date.valueOf(loan.lentOn()),
                Date.valueOf(loan.dueOn()), loan.sourceReference(), actorSub, Timestamp.from(clock.instant()));
    }

    @Override
    public void closeLoan(LibraryCopy.Loan loan, String actorSub) {
        int changed = jdbcTemplate.update("""
                UPDATE library_loan
                SET returned_on = ?, closed_by = ?, closed_reference = ?
                WHERE loan_id = ? AND returned_on IS NULL
                """,
                Date.valueOf(loan.returnedOn()), actorSub, loan.sourceReference(), loan.loanId());
        // A competing return may have closed the same row between the service read and this write. Reporting success
        // then would hand the caller a return that was never persisted, so the lost update is surfaced as a conflict.
        if (changed != 1) {
            throw new IllegalStateException("the loan was already returned");
        }
    }

    @Override
    public List<LibraryCopy.Loan> loansOf(String borrowerUserId, int limit) {
        return jdbcTemplate.query("""
                SELECT loan_id, copy_id, borrower_user_id, lent_on, due_on, returned_on, source_reference
                FROM library_loan
                WHERE borrower_user_id = ?
                ORDER BY lent_on DESC, loan_id
                LIMIT ?
                """, (rs, index) -> loan(rs), borrowerUserId, limit);
    }

    @Override
    public List<LibraryCopy.Loan> openLoans(int limit) {
        return jdbcTemplate.query("""
                SELECT loan_id, copy_id, borrower_user_id, lent_on, due_on, returned_on, source_reference
                FROM library_loan
                WHERE returned_on IS NULL
                ORDER BY due_on, loan_id
                LIMIT ?
                """, (rs, index) -> loan(rs), limit);
    }

    @Override
    public void markCopyWithdrawn(String copyId, String actorSub, String reference, java.time.Instant at) {
        int changed = jdbcTemplate.update("""
                UPDATE library_copy
                SET active = FALSE, withdrawn_by = ?, withdrawn_reference = ?, withdrawn_at = ?
                WHERE copy_id = ? AND active = TRUE
                """,
                actorSub, reference, Timestamp.from(at), copyId);
        if (changed != 1) {
            throw new IllegalStateException("the copy was already withdrawn");
        }
    }

    private static LibraryCopy copy(ResultSet rs) throws SQLException {
        return new LibraryCopy(rs.getString("copy_id"), rs.getString("title_id"), rs.getString("barcode"),
                rs.getString("location"), rs.getBoolean("active"), rs.getString("source_reference"));
    }

    private static LibraryCopy.Loan loan(ResultSet rs) throws SQLException {
        Date returnedOn = rs.getDate("returned_on");
        return new LibraryCopy.Loan(rs.getString("loan_id"), rs.getString("copy_id"),
                rs.getString("borrower_user_id"), rs.getDate("lent_on").toLocalDate(),
                rs.getDate("due_on").toLocalDate(),
                returnedOn == null ? null : returnedOn.toLocalDate(), rs.getString("source_reference"));
    }

    private static final class TitleAccumulator {
        private final String titleId;
        private final String title;
        private final String edition;
        private final Integer publicationYear;
        private final String sourceReference;
        private final List<String> authors = new ArrayList<>();

        private TitleAccumulator(ResultSet rs) throws SQLException {
            titleId = rs.getString("title_id");
            title = rs.getString("title");
            edition = rs.getString("edition");
            int year = rs.getInt("publication_year");
            publicationYear = rs.wasNull() ? null : year;
            sourceReference = rs.getString("source_reference");
        }

        private LibraryTitle toTitle() {
            return new LibraryTitle(titleId, title, authors, edition, publicationYear, sourceReference);
        }
    }
}