package com.jobtracker.application;

import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.common.BaseIntegrationTest;
import com.jobtracker.user.dto.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationQueryIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/applications should paginate, filter by status, and search case-insensitively")
    void shouldPaginateFilterAndSearchApplications() throws Exception {
        AuthResponse user1 = createAndAuthenticateUser("user1@example.com", "User One", "Password123!");
        AuthResponse user2 = createAndAuthenticateUser("user2@example.com", "User Two", "Password123!");

        // Create applications for User 1
        createApp(user1.token(), "Google", "Senior Software Engineer", ApplicationStatus.INTERVIEW);
        createApp(user1.token(), "Amazon", "Backend Developer", ApplicationStatus.APPLIED);
        createApp(user1.token(), "Microsoft", "Azure Cloud Architect", ApplicationStatus.WISHLIST);
        createApp(user1.token(), "Meta", "Production Engineer", ApplicationStatus.OFFER);
        createApp(user1.token(), "Netflix", "Data Platform Engineer", ApplicationStatus.INTERVIEW);

        // Create application for User 2 (must never appear in User 1's results)
        createApp(user2.token(), "Google", "Product Manager", ApplicationStatus.APPLIED);

        // 1. Unfiltered list for user 1 -> 5 items
        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5));

        // 2. Filter by status = INTERVIEW -> 2 items (Google, Netflix)
        mockMvc.perform(get("/api/v1/applications?status=INTERVIEW")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].status").value("INTERVIEW"))
                .andExpect(jsonPath("$.content[1].status").value("INTERVIEW"));

        // 3. Search by company case-insensitive (e.g. "goog") -> 1 item for User 1 (not User 2's Google application)
        mockMvc.perform(get("/api/v1/applications?search=goog")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].company").value("Google"))
                .andExpect(jsonPath("$.content[0].position").value("Senior Software Engineer"));

        // 4. Search by position case-insensitive (e.g. "engineer") -> 4 items for User 1
        mockMvc.perform(get("/api/v1/applications?search=engineer")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)));

        // 5. Pagination (page=0, size=2)
        mockMvc.perform(get("/api/v1/applications?page=0&size=2")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/applications/stats should return counts per status for current user only")
    void shouldReturnCorrectStatsPerUser() throws Exception {
        AuthResponse user1 = createAndAuthenticateUser("stats1@example.com", "Stats User 1", "Password123!");
        AuthResponse user2 = createAndAuthenticateUser("stats2@example.com", "Stats User 2", "Password123!");

        // User 1 applications: 2 APPLIED, 1 INTERVIEW, 1 OFFER
        createApp(user1.token(), "Comp A", "Dev", ApplicationStatus.APPLIED);
        createApp(user1.token(), "Comp B", "Dev", ApplicationStatus.APPLIED);
        createApp(user1.token(), "Comp C", "Dev", ApplicationStatus.INTERVIEW);
        createApp(user1.token(), "Comp D", "Dev", ApplicationStatus.OFFER);

        // User 2 applications: 3 WISHLIST
        createApp(user2.token(), "Comp X", "Dev", ApplicationStatus.WISHLIST);
        createApp(user2.token(), "Comp Y", "Dev", ApplicationStatus.WISHLIST);
        createApp(user2.token(), "Comp Z", "Dev", ApplicationStatus.WISHLIST);

        // Check user 1 stats
        mockMvc.perform(get("/api/v1/applications/stats")
                        .header("Authorization", "Bearer " + user1.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlist").value(0))
                .andExpect(jsonPath("$.applied").value(2))
                .andExpect(jsonPath("$.interview").value(1))
                .andExpect(jsonPath("$.offer").value(1))
                .andExpect(jsonPath("$.rejected").value(0))
                .andExpect(jsonPath("$.total").value(4));

        // Check user 2 stats
        mockMvc.perform(get("/api/v1/applications/stats")
                        .header("Authorization", "Bearer " + user2.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wishlist").value(3))
                .andExpect(jsonPath("$.applied").value(0))
                .andExpect(jsonPath("$.total").value(3));
    }

    private void createApp(String token, String company, String position, ApplicationStatus status) throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(
                company, position, status, null, null, LocalDate.now()
        );
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
