package com.jobtracker.application;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.dto.UpdateStatusRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.common.BaseIntegrationTest;
import com.jobtracker.user.dto.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationCrudIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/v1/applications should create application and return 201 Created")
    void shouldCreateApplicationSuccessfully() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("owner@example.com", "Owner User", "Password123!");

        CreateApplicationRequest request = new CreateApplicationRequest(
                "Google", "Software Engineer III", ApplicationStatus.APPLIED,
                "https://careers.google.com/jobs/123", "Referred by friend", LocalDate.of(2026, 10, 20)
        );

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.company").value("Google"))
                .andExpect(jsonPath("$.position").value("Software Engineer III"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.link").value("https://careers.google.com/jobs/123"))
                .andExpect(jsonPath("$.notes").value("Referred by friend"))
                .andExpect(jsonPath("$.appliedAt").value("2026-10-20"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} should return application when owned by requester")
    void shouldReturnApplicationWhenOwned() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("owner@example.com", "Owner User", "Password123!");

        CreateApplicationRequest request = new CreateApplicationRequest(
                "Stripe", "Backend Engineer", ApplicationStatus.WISHLIST, null, "Great culture", null
        );

        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        mockMvc.perform(get("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.company").value("Stripe"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id} should return 404 Not Found when accessed by another user")
    void shouldReturn404WhenApplicationBelongsToAnotherUser() throws Exception {
        AuthResponse ownerAuth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");
        AuthResponse strangerAuth = createAndAuthenticateUser("stranger@example.com", "Stranger", "Password123!");

        CreateApplicationRequest request = new CreateApplicationRequest(
                "Netflix", "Senior Platform Engineer", ApplicationStatus.INTERVIEW, null, "Confidential", null
        );

        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + ownerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        // Stranger attempts to view owner's application -> 404 to avoid leaking existence
        mockMvc.perform(get("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + strangerAuth.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Application not found")));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} should update application for owner")
    void shouldUpdateApplicationSuccessfully() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Airbnb", "Software Engineer", ApplicationStatus.WISHLIST, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        UpdateApplicationRequest updateReq = new UpdateApplicationRequest(
                "Airbnb Inc.", "Senior Software Engineer", ApplicationStatus.APPLIED,
                "https://airbnb.com/careers/1", "Applied via job portal", LocalDate.of(2026, 10, 21)
        );

        mockMvc.perform(put("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Airbnb Inc."))
                .andExpect(jsonPath("$.position").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.link").value("https://airbnb.com/careers/1"))
                .andExpect(jsonPath("$.notes").value("Applied via job portal"))
                .andExpect(jsonPath("$.appliedAt").value("2026-10-21"));
    }

    @Test
    @DisplayName("PUT /api/v1/applications/{id} should return 404 when updating another user's application")
    void shouldReturn404WhenUpdatingOtherUsersApplication() throws Exception {
        AuthResponse ownerAuth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");
        AuthResponse strangerAuth = createAndAuthenticateUser("stranger@example.com", "Stranger", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Apple", "iOS Dev", ApplicationStatus.WISHLIST, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + ownerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        UpdateApplicationRequest updateReq = new UpdateApplicationRequest(
                "Malicious Corp", "Hacker", ApplicationStatus.OFFER, null, null, null
        );

        mockMvc.perform(put("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + strangerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status should update column status")
    void shouldUpdateStatusSuccessfully() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Microsoft", "Cloud Architect", ApplicationStatus.APPLIED, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        UpdateStatusRequest statusReq = new UpdateStatusRequest(ApplicationStatus.INTERVIEW);

        mockMvc.perform(patch("/api/v1/applications/" + created.id() + "/status")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    @DisplayName("PATCH /api/v1/applications/{id}/status should return 404 when modifying another user's application")
    void shouldReturn404WhenPatchingOtherUsersApplication() throws Exception {
        AuthResponse ownerAuth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");
        AuthResponse strangerAuth = createAndAuthenticateUser("stranger@example.com", "Stranger", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Microsoft", "Cloud Architect", ApplicationStatus.APPLIED, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + ownerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        UpdateStatusRequest statusReq = new UpdateStatusRequest(ApplicationStatus.OFFER);

        mockMvc.perform(patch("/api/v1/applications/" + created.id() + "/status")
                        .header("Authorization", "Bearer " + strangerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} should return 204 No Content for owner")
    void shouldDeleteApplicationSuccessfully() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Uber", "Backend Engineer", ApplicationStatus.REJECTED, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        mockMvc.perform(delete("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isNoContent());

        assertThat(applicationRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/applications/{id} should return 404 when deleting another user's application")
    void shouldReturn404WhenDeletingOtherUsersApplication() throws Exception {
        AuthResponse ownerAuth = createAndAuthenticateUser("owner@example.com", "Owner", "Password123!");
        AuthResponse strangerAuth = createAndAuthenticateUser("stranger@example.com", "Stranger", "Password123!");

        CreateApplicationRequest createReq = new CreateApplicationRequest(
                "Uber", "Backend Engineer", ApplicationStatus.REJECTED, null, null, null
        );
        String createJson = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + ownerAuth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ApplicationResponse created = objectMapper.readValue(createJson, ApplicationResponse.class);

        mockMvc.perform(delete("/api/v1/applications/" + created.id())
                        .header("Authorization", "Bearer " + strangerAuth.token()))
                .andExpect(status().isNotFound());

        // Verify entity was NOT deleted
        assertThat(applicationRepository.findById(created.id())).isPresent();
    }
}
