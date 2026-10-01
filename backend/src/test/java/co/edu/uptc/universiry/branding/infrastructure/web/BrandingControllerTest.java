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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=30")))
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
                .andExpect(jsonPath("$.modules.length()").value(8))
                .andExpect(jsonPath("$.modules[0].key").value("home"))
                .andExpect(jsonPath("$.modules[6].key").value("visual-identity"))
                .andExpect(jsonPath("$.modules[7].available").value(true))
                .andExpect(jsonPath("$.modules[7].visible").value(true))
                .andExpect(jsonPath("$.modules[7].key").value("admissions"))
                .andExpect(jsonPath("$.banners.length()").value(0))
                .andExpect(jsonPath("$.actor").doesNotExist())
                .andExpect(jsonPath("$.permissions").doesNotExist())
                .andExpect(jsonPath("$.audit").doesNotExist())
                .andExpect(jsonPath("$.assets.logoLightPath").doesNotExist());
    }

    @Test
    void same_etag_returns_not_modified_without_a_response_body() throws Exception {
        mockMvc.perform(get("/api/v1/branding").header("If-None-Match", "W/\"1\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=30")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
    }
}
