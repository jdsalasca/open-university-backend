package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.identity.application.CurrentIdentityService;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.ScopeKind;
import co.edu.uptc.universiry.identity.infrastructure.web.AuthenticatedPrincipalWebMapper;
import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService;
import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService.InstitutionalNoticePage;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;
import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
public class MyInstitutionalNoticeController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InstitutionalNoticeService notices;
    private final CurrentIdentityService identities;
    private final Clock clock;

    public MyInstitutionalNoticeController(
            InstitutionalNoticeService notices, CurrentIdentityService identities, Clock clock) {
        this.notices = notices;
        this.identities = identities;
        this.clock = clock;
    }

    @GetMapping("/api/v1/notices")
    public ResponseEntity<VisibleInstitutionalNoticeResponse.VisibleNoticesResponse> myNotices(
            @RequestParam(defaultValue = "25") int limit,
            Authentication authentication,
            HttpServletRequest request) {
        var principal = AuthenticatedPrincipalWebMapper.from(authentication);
        var permissions = AuthenticatedPrincipalWebMapper.applicationPermissions(authentication.getAuthorities());
        var snapshot = identities.currentIdentity(principal, permissions);

        InstitutionalNoticePage page = notices.visibleOn(LocalDate.now(clock), audiencesOf(snapshot.assignments()), limit);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new VisibleInstitutionalNoticeResponse.VisibleNoticesResponse(
                        page.notices().stream().map(VisibleInstitutionalNoticeResponse::from).toList()));
    }

    /**
     * The caller's audience comes from the scopes of their own active role assignments, which are audited access
     * grants. A job appointment is deliberately not a notice audience: it describes a person, not a community.
     */
    private static List<NoticeAudience> audiencesOf(List<RoleAssignment> assignments) {
        Set<NoticeAudience> audiences = new LinkedHashSet<>();
        for (RoleAssignment assignment : assignments) {
            for (AssignmentScope scope : assignment.scopes()) {
                NoticeAudienceKind kind = audienceKindOf(scope.kind());
                if (kind != null) {
                    audiences.add(new NoticeAudience(kind, scope.stableReference()));
                }
            }
        }
        return List.copyOf(audiences);
    }

    private static NoticeAudienceKind audienceKindOf(ScopeKind scopeKind) {
        return switch (scopeKind) {
            case UNIVERSITY -> NoticeAudienceKind.UNIVERSITY;
            case SITE -> NoticeAudienceKind.SITE;
            case FACULTY -> NoticeAudienceKind.FACULTY;
            case PROGRAM -> NoticeAudienceKind.PROGRAM;
            case JOB_APPOINTMENT -> null;
        };
    }
}