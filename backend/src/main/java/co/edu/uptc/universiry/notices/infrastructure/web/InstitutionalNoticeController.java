package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService;
import co.edu.uptc.universiry.notices.application.PublishInstitutionalNoticeCommand;
import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class InstitutionalNoticeController {

    private final InstitutionalNoticeService service;

    public InstitutionalNoticeController(InstitutionalNoticeService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/notices")
    public InstitutionalNoticePageResponse recent(@RequestParam(defaultValue = "25") int limit) {
        return InstitutionalNoticePageResponse.from(service.recent(limit));
    }

    @PostMapping("/api/v1/admin/notices")
    public ResponseEntity<InstitutionalNoticeResponse> publish(
            @Valid @RequestBody PublishInstitutionalNoticeRequest request,
            Authentication authentication) {
        var command = new PublishInstitutionalNoticeCommand(
                request.title(),
                request.body(),
                request.sourceReference(),
                request.publishedFrom(),
                request.publishedThrough(),
                request.audiences().stream()
                        .map(audience -> new NoticeAudience(audience.kind(), audience.reference()))
                        .toList());
        InstitutionalNotice published = service.publish(command, authentication.getName());
        return ResponseEntity.status(CREATED).body(InstitutionalNoticeResponse.from(published));
    }
}