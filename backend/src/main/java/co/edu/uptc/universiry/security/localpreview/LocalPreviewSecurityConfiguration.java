package co.edu.uptc.universiry.security.localpreview;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("local-preview")
public class LocalPreviewSecurityConfiguration {

    @Bean
    SecureRandom localPreviewSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    LocalPreviewSessionService localPreviewSessionService(Clock clock, SecureRandom localPreviewSecureRandom) {
        return new InMemoryLocalPreviewSessionService(clock, localPreviewSecureRandom,
                Duration.ofHours(4), 16);
    }

    @Bean
    JwtDecoder localPreviewJwtDecoder(LocalPreviewSessionService sessions) {
        return sessions::decode;
    }

    @Bean
    JwtAuthenticationConverter localPreviewJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new LocalPreviewAuthoritiesConverter());
        return converter;
    }
}
