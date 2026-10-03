package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.library.domain.LibraryCopy;

public record LibraryCopyResponse(
        String copyId,
        String titleId,
        String barcode,
        String location,
        boolean active
) {

    public static LibraryCopyResponse from(LibraryCopy copy) {
        return new LibraryCopyResponse(copy.copyId(), copy.titleId(), copy.barcode(), copy.location(), copy.active());
    }
}