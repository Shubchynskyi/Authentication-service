package com.authenticationservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.authenticationservice.config.BaseIntegrationTest;
import com.authenticationservice.config.TestConfig;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.constants.TestConstants;
import com.authenticationservice.model.MaskedLoginSettings;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.MaskedLoginSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = true)
@Transactional
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.yml")
@Import(TestConfig.class)
@DisplayName("MaskedLogin public/admin integration tests")
class MaskedLoginControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaskedLoginSettingsRepository maskedLoginSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = getTransactionTemplate();
        tx.execute(status -> {
            cleanupTestData();
            ensureRolesExist();

            // Ensure masked login settings exist
            MaskedLoginSettings settings = maskedLoginSettingsRepository.findById(1L)
                    .orElseGet(MaskedLoginSettings::new);
            settings.setId(1L);
            settings.setEnabled(false);
            settings.setTemplateId(1);
            maskedLoginSettingsRepository.saveAndFlush(settings);

            // Create admin user
            adminUser = createAdminUser();
            adminUser.setPassword(passwordEncoder.encode(TestConstants.TestData.ADMIN_PASSWORD));
            userRepository.saveAndFlush(adminUser);
            return null;
        });
    }

    @Test
    @DisplayName("Public GET returns masked login settings")
    void shouldReturnPublicMaskedLoginSettings() throws Exception {
        TransactionTemplate tx = getTransactionTemplate();
        tx.execute(status -> {
            MaskedLoginSettings settings = maskedLoginSettingsRepository.findById(1L).orElseThrow();
            settings.setEnabled(true);
            settings.setTemplateId(4);
            maskedLoginSettingsRepository.saveAndFlush(settings);
            return null;
        });

        mockMvc.perform(get("/api/public/masked-login/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.templateId").value(4));
    }

    @Test
    @DisplayName("Admin PUT updates masked login settings with valid password")
    void shouldUpdateSettingsWithValidPassword() throws Exception {
        var requestBody = objectMapper.createObjectNode()
                .put("enabled", true)
                .put("templateId", 3)
                .put("password", TestConstants.TestData.ADMIN_PASSWORD);

        mockMvc.perform(put("/api/admin/masked-login/settings")
                        .with(user(adminUser.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isOk());

        TransactionTemplate tx = getTransactionTemplate();
        tx.execute(status -> {
            MaskedLoginSettings updated = maskedLoginSettingsRepository.findById(1L).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(updated.getEnabled()).isTrue();
            org.assertj.core.api.Assertions.assertThat(updated.getTemplateId()).isEqualTo(3);
            return null;
        });
    }

    @Test
    @DisplayName("Admin PUT fails with invalid password")
    void shouldFailUpdateWithInvalidPassword() throws Exception {
        var requestBody = objectMapper.createObjectNode()
                .put("enabled", true)
                .put("templateId", 2)
                .put("password", "WrongPass123@");

        mockMvc.perform(put("/api/admin/masked-login/settings")
                        .with(user(adminUser.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(MessageConstants.INVALID_PASSWORD));
    }
}

