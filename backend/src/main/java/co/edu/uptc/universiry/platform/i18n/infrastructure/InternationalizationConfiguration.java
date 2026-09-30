package co.edu.uptc.universiry.platform.i18n.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class InternationalizationConfiguration {

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("es-CO"));
        return resolver;
    }
}
