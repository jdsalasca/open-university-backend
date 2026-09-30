package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicPeriodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class AcademicPeriodController {

    private final AcademicPeriodService service;

    public AcademicPeriodController(AcademicPeriodService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/academic-periods")
    public List<AcademicPeriodResponse> publicPeriods() {
        return service.publicPeriods().stream().map(AcademicPeriodResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/academic-periods")
    public List<AcademicPeriodResponse> adminPeriods() {
        return service.adminPeriods().stream().map(AcademicPeriodResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/academic-periods/{periodId}/history")
    public AcademicPeriodHistoryResponse history(@PathVariable UUID periodId) {
        return AcademicPeriodHistoryResponse.from(service.history(periodId));
    }

    @PostMapping("/api/v1/admin/academic-periods")
    public ResponseEntity<AcademicPeriodResponse> createPeriod(
            @Valid @RequestBody CreateAcademicPeriodRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
                .body(AcademicPeriodResponse.from(service.createPeriod(request.toCommand(), authentication.getName())));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/calendars")
    public ResponseEntity<AcademicCalendarRevisionResponse> createCalendar(
            @PathVariable UUID periodId,
            @Valid @RequestBody CreateAcademicCalendarRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(CREATED).body(AcademicCalendarRevisionResponse.from(
                service.createCalendar(periodId, request.toCommand(), authentication.getName())));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/calendars/{revisionId}/publish")
    public AcademicCalendarRevisionResponse publishCalendar(
            @PathVariable UUID periodId,
            @PathVariable UUID revisionId,
            Authentication authentication
    ) {
        return AcademicCalendarRevisionResponse.from(
                service.publishCalendar(periodId, revisionId, authentication.getName()));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/calendars/{revisionId}/activate")
    public AcademicPeriodResponse activateCalendarRevision(
            @PathVariable UUID periodId,
            @PathVariable UUID revisionId,
            Authentication authentication
    ) {
        return AcademicPeriodResponse.from(
                service.activateCalendarRevision(periodId, revisionId, authentication.getName()));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/approve")
    public AcademicPeriodResponse approve(
            @PathVariable UUID periodId,
            @Valid @RequestBody ApproveAcademicPeriodRequest request,
            Authentication authentication
    ) {
        return AcademicPeriodResponse.from(service.approve(periodId, request.calendarRevisionId(),
                request.approvalReference(), authentication.getName()));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/open")
    public AcademicPeriodResponse open(@PathVariable UUID periodId, Authentication authentication) {
        return AcademicPeriodResponse.from(service.open(periodId, authentication.getName()));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/close")
    public AcademicPeriodResponse close(@PathVariable UUID periodId, Authentication authentication) {
        return AcademicPeriodResponse.from(service.close(periodId, authentication.getName()));
    }

    @PostMapping("/api/v1/admin/academic-periods/{periodId}/cancel")
    public AcademicPeriodResponse cancel(
            @PathVariable UUID periodId,
            @Valid @RequestBody CancelAcademicPeriodRequest request,
            Authentication authentication
    ) {
        return AcademicPeriodResponse.from(service.cancel(periodId, request.reference(), authentication.getName()));
    }
}
