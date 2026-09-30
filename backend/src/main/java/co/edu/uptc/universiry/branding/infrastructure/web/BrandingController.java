package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.BrandingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branding")
public class BrandingController {

    private final BrandingQueryService brandingQueryService;

    public BrandingController(BrandingQueryService brandingQueryService) {
        this.brandingQueryService = brandingQueryService;
    }

    @GetMapping
    public BrandingResponse getPublicConfiguration() {
        return BrandingResponse.from(brandingQueryService.currentPublicConfiguration());
    }
}
