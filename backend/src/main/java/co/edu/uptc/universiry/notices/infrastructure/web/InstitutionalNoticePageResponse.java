package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService.InstitutionalNoticePage;
import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;

import java.util.List;

public record InstitutionalNoticePageResponse(List<InstitutionalNoticeResponse> notices) {

    public static InstitutionalNoticePageResponse from(InstitutionalNoticePage page) {
        return new InstitutionalNoticePageResponse(page.notices().stream()
                .map(InstitutionalNoticeResponse::from)
                .toList());
    }
}