package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.Actor;
import co.edu.uptc.universiry.branding.application.BrandAssetStorageException;
import co.edu.uptc.universiry.branding.application.BrandAssetService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@RestController
public class BrandAssetController {

    private final BrandAssetService assetService;

    public BrandAssetController(BrandAssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping(path = "/api/v1/admin/branding/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandAssetService.StoredAsset> upload(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            BrandAssetService.StoredAsset asset = assetService.store(file.getBytes(), new Actor(authentication.getName()));
            return ResponseEntity.created(URI.create("/assets/" + asset.assetId())).body(asset);
        } catch (IOException exception) {
            throw new BrandAssetStorageException("The uploaded image could not be read.", exception);
        }
    }

    @GetMapping("/assets/{assetId}")
    public ResponseEntity<byte[]> openPublishedAsset(@PathVariable String assetId) {
        BrandAssetService.AssetContent content = assetService.openPublished(assetId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(content.mimeType()));
        headers.setETag("\"" + content.sha256() + "\"");
        headers.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'none'; sandbox");
        return new ResponseEntity<>(content.bytes(), headers, HttpStatus.OK);
    }
}
