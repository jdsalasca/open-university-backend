package co.edu.uptc.universiry.branding.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returns_official_branding_configuration() throws Exception {
        mockMvc.perform(get("/api/v1/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colors.primary").value("#FFCC29"))
                .andExpect(jsonPath("$.colors.ink").value("#1A1A1A"))
                .andExpect(jsonPath("$.colors.surface").value("#FFFFFF"))
                .andExpect(jsonPath("$.colors.text").value("#1A1A1A"))
                .andExpect(jsonPath("$.colors.accent").value("#FFCC29"))
                .andExpect(jsonPath("$.colors.focus").value("#1A1A1A"))
                .andExpect(jsonPath("$.revision").value(1))
                .andExpect(jsonPath("$.institutionName").value("Universidad Pedagógica y Tecnológica de Colombia"))
                .andExpect(jsonPath("$.assets.logoLight").value(nullValue()))
                .andExpect(jsonPath("$.assets.logoDark").value(nullValue()))
                .andExpect(jsonPath("$.assets.favicon").value(nullValue()))
                .andExpect(jsonPath("$.modules.length()").value(7))
                .andExpect(jsonPath("$.modules[0].key").value("home"))
                .andExpect(jsonPath("$.modules[6].key").value("visual-identity"))
                .andExpect(jsonPath("$.modules[6].available").value(true))
                .andExpect(jsonPath("$.modules[6].visible").value(true))
                .andExpect(jsonPath("$.banners.length()").value(0))
                .andExpect(jsonPath("$.actor").doesNotExist())
                .andExpect(jsonPath("$.permissions").doesNotExist())
                .andExpect(jsonPath("$.audit").doesNotExist())
                .andExpect(jsonPath("$.assets.logoLightPath").doesNotExist());
    }
}
