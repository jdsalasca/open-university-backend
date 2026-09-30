package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.Actor;
import co.edu.uptc.universiry.branding.application.BrandingService;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/branding")
public class BrandingAdministrationController {

    private final BrandingService brandingService;

    public BrandingAdministrationController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping
    public BrandingResponse getCurrentConfiguration() {
        return BrandingResponse.from(brandingService.currentAdministrativeConfiguration());
    }

    @PutMapping
    public BrandingResponse publish(@Valid @RequestBody BrandingChangeRequest request, Authentication authentication) {
        BrandingConfiguration configuration = brandingService.publish(
                request.toChange(),
                new Actor(authentication.getName()),
                request.expectedRevision()
        );
        return BrandingResponse.from(configuration);
    }

    @PostMapping("/rollback")
    public BrandingResponse rollback(@Valid @RequestBody BrandingRollbackRequest request, Authentication authentication) {
        BrandingConfiguration configuration = brandingService.restore(
                request.targetRevision(),
                new Actor(authentication.getName()),
                request.expectedRevision()
        );
        return BrandingResponse.from(configuration);
    }
}
