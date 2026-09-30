package co.edu.uptc.universiry.academics.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class AcademicTimeConfiguration {
    @Bean
    Clock academicClock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
