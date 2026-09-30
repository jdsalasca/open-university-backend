package co.edu.uptc.universiry.academics.application;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public record CurriculumDraftCursor(LocalDateTime createdAt, UUID curriculumId) {

    private static final int MAX_ENCODED_LENGTH = 256;

    public CurriculumDraftCursor {
        if (createdAt == null || curriculumId == null) {
            throw new InvalidCurriculumDraftsPageQueryException();
        }
    }

    public String encode() {
        String value = createdAt + "\n" + curriculumId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static CurriculumDraftCursor decode(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > MAX_ENCODED_LENGTH) {
            throw new InvalidCurriculumDraftsPageQueryException();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int separator = decoded.indexOf('\n');
            if (separator <= 0 || separator != decoded.lastIndexOf('\n')) {
                throw new InvalidCurriculumDraftsPageQueryException();
            }

            LocalDateTime createdAt = LocalDateTime.parse(decoded.substring(0, separator));
            String curriculumIdText = decoded.substring(separator + 1);
            UUID curriculumId = UUID.fromString(curriculumIdText);
            if (!curriculumId.toString().equals(curriculumIdText)) {
                throw new InvalidCurriculumDraftsPageQueryException();
            }
            return new CurriculumDraftCursor(createdAt, curriculumId);
        } catch (IllegalArgumentException | DateTimeParseException invalidCursor) {
            throw new InvalidCurriculumDraftsPageQueryException();
        }
    }
}
