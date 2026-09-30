package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.BrandingQueryService;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/branding")
public class BrandingController {

    private final BrandingQueryService brandingQueryService;

    public BrandingController(BrandingQueryService brandingQueryService) {
        this.brandingQueryService = brandingQueryService;
    }

    @GetMapping
    public ResponseEntity<BrandingResponse> getPublicConfiguration(HttpServletRequest request) {
        BrandingConfiguration configuration = brandingQueryService.currentPublicConfiguration();
        String etag = "\"" + configuration.revision() + "\"";
        HttpHeaders headers = new HttpHeaders();
        headers.setETag(etag);
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic().mustRevalidate());

        if (matchesIfNoneMatch(request.getHeader(HttpHeaders.IF_NONE_MATCH), etag)) {
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_MODIFIED);
        }
        return new ResponseEntity<>(BrandingResponse.from(configuration), headers, HttpStatus.OK);
    }

    private boolean matchesIfNoneMatch(String header, String currentEtag) {
        if (header == null || header.isBlank()) {
            return false;
        }
        return Arrays.stream(header.split(","))
                .map(String::strip)
                .map(candidate -> candidate.startsWith("W/") ? candidate.substring(2).strip() : candidate)
                .anyMatch(candidate -> candidate.equals("*") || candidate.equals(currentEtag));
    }
}
