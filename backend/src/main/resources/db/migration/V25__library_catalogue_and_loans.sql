-- Bibliography and loans. A copy is the lendable unit and a loan always points at the canonical university user.
-- The due date is stored as decided by the lending unit: the platform never invents a loan period.

CREATE TABLE library_title (
    title_id CHAR(36) NOT NULL,
    title VARCHAR(240) NOT NULL,
    edition VARCHAR(80) NOT NULL,
    publication_year SMALLINT NULL,
    source_reference VARCHAR(240) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (title_id),
    CONSTRAINT ck_library_title_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_library_title_edition CHECK (CHAR_LENGTH(TRIM(edition)) > 0),
    CONSTRAINT ck_library_title_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1450 AND 2200),
    CONSTRAINT ck_library_title_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_library_title_actor CHECK (CHAR_LENGTH(TRIM(created_by)) > 0),
    INDEX ix_library_title_name (title)
);

CREATE TABLE library_title_author (
    title_id CHAR(36) NOT NULL,
    author_order INT NOT NULL,
    author_name VARCHAR(160) NOT NULL,
    PRIMARY KEY (title_id, author_order),
    CONSTRAINT fk_library_title_author_title FOREIGN KEY (title_id) REFERENCES library_title (title_id),
    CONSTRAINT ck_library_title_author_order CHECK (author_order >= 0 AND author_order < 20),
    CONSTRAINT ck_library_title_author_name CHECK (CHAR_LENGTH(TRIM(author_name)) > 0)
);

CREATE TABLE library_copy (
    copy_id CHAR(36) NOT NULL,
    title_id CHAR(36) NOT NULL,
    barcode VARCHAR(48) NOT NULL,
    location VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source_reference VARCHAR(240) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (copy_id),
    CONSTRAINT uq_library_copy_barcode UNIQUE (barcode),
    CONSTRAINT fk_library_copy_title FOREIGN KEY (title_id) REFERENCES library_title (title_id),
    CONSTRAINT ck_library_copy_barcode CHECK (CHAR_LENGTH(TRIM(barcode)) > 0),
    CONSTRAINT ck_library_copy_location CHECK (CHAR_LENGTH(TRIM(location)) > 0),
    CONSTRAINT ck_library_copy_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_library_copy_actor CHECK (CHAR_LENGTH(TRIM(created_by)) > 0),
    INDEX ix_library_copy_title (title_id)
);

CREATE TABLE library_loan (
    loan_id CHAR(36) NOT NULL,
    copy_id CHAR(36) NOT NULL,
    borrower_user_id CHAR(36) NOT NULL,
    lent_on DATE NOT NULL,
    due_on DATE NOT NULL,
    returned_on DATE NULL,
    source_reference VARCHAR(240) NOT NULL,
    closed_by VARCHAR(255) NULL,
    closed_reference VARCHAR(240) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (loan_id),
    CONSTRAINT fk_library_loan_copy FOREIGN KEY (copy_id) REFERENCES library_copy (copy_id),
    CONSTRAINT fk_library_loan_borrower FOREIGN KEY (borrower_user_id) REFERENCES university_user (user_id),
    CONSTRAINT ck_library_loan_window CHECK (due_on >= lent_on),
    CONSTRAINT ck_library_loan_return CHECK (returned_on IS NULL OR returned_on >= lent_on),
    -- An open loan carries no closure trail, and a closed loan carries both parts of it.
    CONSTRAINT ck_library_loan_closure CHECK (
        (returned_on IS NULL AND closed_by IS NULL AND closed_reference IS NULL)
        OR (returned_on IS NOT NULL AND CHAR_LENGTH(TRIM(closed_by)) > 0
            AND CHAR_LENGTH(TRIM(closed_reference)) > 0)
    ),
    -- At most one open loan per copy is enforced by the application: MySQL treats NULL as a distinct value in a
    -- unique index, so a unique key on (copy_id, returned_on) cannot express "no other open loan". The repository
    -- locks the copy row and rejects a second open loan inside the same transaction.
    INDEX ix_library_loan_open_copy (copy_id, returned_on),
    INDEX ix_library_loan_borrower (borrower_user_id, lent_on)
);