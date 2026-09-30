package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBrandingQueryServiceTest {

    @Test
    void serves_the_persisted_public_configuration() {
        // Arrange
        BrandingRepository repository = mock(BrandingRepository.class);
        BrandingConfiguration persistedConfiguration = BrandingConfiguration.defaults();
        when(repository.findCurrentPublic(any())).thenReturn(Optional.of(persistedConfiguration));
        DefaultBrandingQueryService service = new DefaultBrandingQueryService(repository);

        // Act
        BrandingConfiguration actual = service.currentPublicConfiguration();

        // Assert
        assertSame(persistedConfiguration, actual);
        verify(repository).findCurrentPublic(any());
    }
}
