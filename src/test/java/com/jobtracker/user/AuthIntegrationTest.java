package com.jobtracker.user;

import com.jobtracker.common.BaseIntegrationTest;
import com.jobtracker.user.dto.AuthResponse;
import com.jobtracker.user.dto.LoginRequest;
import com.jobtracker.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/v1/auth/register should create user and return 201 Created with JWT")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("john.doe@example.com", "Secret123!", "John Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.user.fullName").value("John Doe"))
                .andExpect(jsonPath("$.user.role").value("ROLE_USER"));

        assertThat(userRepository.findByEmail("john.doe@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 Conflict when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        createAndAuthenticateUser("existing@example.com", "Existing User", "Password123!");

        RegisterRequest duplicateRequest = new RegisterRequest("existing@example.com", "AnotherPassword!", "New Name");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 Bad Request on validation errors")
    void shouldReturn400OnInvalidRegistrationData() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("invalid-email", "123", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.fullName", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should authenticate and return 200 OK with JWT")
    void shouldLoginSuccessfully() throws Exception {
        createAndAuthenticateUser("jane@example.com", "Jane Doe", "Secret123!");

        LoginRequest loginRequest = new LoginRequest("jane@example.com", "Secret123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 Unauthorized for incorrect password")
    void shouldReturn401OnWrongPassword() throws Exception {
        createAndAuthenticateUser("jane@example.com", "Jane Doe", "Secret123!");

        LoginRequest loginRequest = new LoginRequest("jane@example.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return current user profile when authenticated")
    void shouldReturnCurrentUserProfileWhenAuthenticated() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("jane@example.com", "Jane Doe", "Secret123!");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 Unauthorized without Bearer token")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
