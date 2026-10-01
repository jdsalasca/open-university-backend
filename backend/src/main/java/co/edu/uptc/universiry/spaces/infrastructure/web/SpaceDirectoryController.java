package co.edu.uptc.universiry.spaces.infrastructure.web;

import co.edu.uptc.universiry.spaces.application.PublicSpaceDirectory;
import co.edu.uptc.universiry.spaces.domain.SpaceDirectorySnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class SpaceDirectoryController {

    private final PublicSpaceDirectory directory;

    public SpaceDirectoryController(PublicSpaceDirectory directory) {
        this.directory = directory;
    }

    @GetMapping("/api/v1/spaces")
    public SpaceDirectorySnapshot listLocations() {
        return directory.snapshot();
    }
}
