package co.edu.uptc.universiry.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the institutional API once, so the generated document always names the product instead of
 * the framework defaults. Reachability is decided by the security chain, never by this class.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiDocumentationConfiguration {

    @Bean
    OpenAPI institutionalPlatformApi() {
        return new OpenAPI().info(new Info()
                .title("Universiry institutional platform API")
                .version("v1")
                .description("""
                        Read and administration endpoints of the institutional platform.

                        Every administrative route is authorized on the server. React only presents capabilities; it never \
                        grants access. Public routes are anonymous; administrative routes require an institutional \
                        session carrying the permission of their own capability family."""));
    }
}