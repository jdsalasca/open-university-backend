package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBrandingQueryServiceTest {

    private static final Clock INSTITUTION_CLOCK =
            Clock.fixed(Instant.parse("2026-10-03T13:00:00Z"), ZoneId.of("America/Bogota"));

    @Test
    void serves_the_persisted_public_configuration() {
        // Arrange
        BrandingRepository repository = mock(BrandingRepository.class);
        BrandingConfiguration persistedConfiguration = BrandingConfiguration.defaults();
        when(repository.findCurrentPublic(any())).thenReturn(Optional.of(persistedConfiguration));
        DefaultBrandingQueryService service = new DefaultBrandingQueryService(repository, INSTITUTION_CLOCK);

        // Act
        BrandingConfiguration actual = service.currentPublicConfiguration();

        // Assert
        assertSame(persistedConfiguration, actual);
        verify(repository).findCurrentPublic(any());
    }

    @Test
    void resolves_public_visibility_with_the_injected_clock() {
        // Arrange: the wall clock must not decide which banners are visible.
        BrandingRepository repository = mock(BrandingRepository.class);
        when(repository.findCurrentPublic(any())).thenReturn(Optional.of(BrandingConfiguration.defaults()));
        DefaultBrandingQueryService service = new DefaultBrandingQueryService(repository, INSTITUTION_CLOCK);

        // Act
        service.currentPublicConfiguration();

        // Assert
        verify(repository).findCurrentPublic(INSTITUTION_CLOCK.instant());
    }
}
