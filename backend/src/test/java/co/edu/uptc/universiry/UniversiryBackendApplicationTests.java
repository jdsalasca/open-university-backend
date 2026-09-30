package co.edu.uptc.universiry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class UniversiryBackendApplicationTests {

	@Autowired
	private JwtAuthenticationConverter jwtAuthenticationConverter;

	@Test
	void contextLoads() {
	}

	@Test
	void application_roles_grant_no_permissions_when_the_institutional_mapping_is_empty() {
		// Arrange
		Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
				.header("alg", "none")
				.claim("authorities", List.of("synthetic.brand.manager", "synthetic.catalog.operator"))
				.build();

		// Act
		var authentication = jwtAuthenticationConverter.convert(jwt);

		// Assert
		assertTrue(authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().startsWith("branding:")
						|| authority.getAuthority().startsWith("academic:")));
	}

}
