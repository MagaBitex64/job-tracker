package com.jobtracker.application;

import com.jobtracker.common.BaseIntegrationTest;
import com.jobtracker.user.dto.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationValidationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/v1/applications should return 400 Bad Request with field errors when fields are blank")
    void shouldReturn400WhenFieldsAreBlank() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("valid@example.com", "Valid User", "Password123!");

        String invalidJson = """
            {
                "company": "",
                "position": "  ",
                "status": null
            }
        """;

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.company", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.position", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.status", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/applications should return 400 when string length exceeds max constraint")
    void shouldReturn400WhenLengthExceeded() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("valid@example.com", "Valid User", "Password123!");

        String tooLongCompany = "A".repeat(121);
        String invalidJson = String.format("""
            {
                "company": "%s",
                "position": "Backend Developer",
                "status": "APPLIED"
            }
        """, tooLongCompany);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.company", containsString("120")));
    }

    @Test
    @DisplayName("POST /api/v1/applications should return 400 on malformed JSON payload")
    void shouldReturn400OnMalformedJson() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("valid@example.com", "Valid User", "Password123!");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed: json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status should return 400 when status is missing")
    void shouldReturn400WhenStatusNullInPatch() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("valid@example.com", "Valid User", "Password123!");

        mockMvc.perform(patch("/api/v1/applications/1/status")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status", notNullValue()));
    }

    @Test
    @DisplayName("Protected endpoints should return 401 when Authorization header is invalid")
    void shouldReturn401OnInvalidJwtToken() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
