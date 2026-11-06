package com.jobtracker.parser;

import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.common.BaseIntegrationTest;
import com.jobtracker.parser.dto.ImportVacancyRequest;
import com.jobtracker.user.dto.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParserIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/parser/search should return 401 when unauthenticated")
    void searchVacancies_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/parser/search")
                        .param("query", "Java"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/parser/import should return 401 when unauthenticated")
    void importVacancy_unauthenticated() throws Exception {
        ImportVacancyRequest request = new ImportVacancyRequest(
                "Yandex", "Java Developer", ApplicationStatus.WISHLIST, "https://hh.ru/1", "Test"
        );

        mockMvc.perform(post("/api/v1/parser/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/parser/import should create application and return 201 Created")
    void importVacancy_success() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("parser_user@example.com", "Parser User", "Password123!");

        ImportVacancyRequest request = new ImportVacancyRequest(
                "Ozon", "Senior Java Engineer", ApplicationStatus.WISHLIST,
                "https://hh.ru/vacancy/999888", "Зарплата от 300 000 руб."
        );

        mockMvc.perform(post("/api/v1/parser/import")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.company").value("Ozon"))
                .andExpect(jsonPath("$.position").value("Senior Java Engineer"))
                .andExpect(jsonPath("$.status").value("WISHLIST"))
                .andExpect(jsonPath("$.link").value("https://hh.ru/vacancy/999888"))
                .andExpect(jsonPath("$.notes").value("Зарплата от 300 000 руб."))
                .andExpect(jsonPath("$.appliedAt", notNullValue()));

        assertThat(applicationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/parser/import should return 400 when company is blank")
    void importVacancy_validationError() throws Exception {
        AuthResponse auth = createAndAuthenticateUser("parser_val@example.com", "Parser User", "Password123!");

        ImportVacancyRequest invalidRequest = new ImportVacancyRequest(
                "", "Java Dev", ApplicationStatus.WISHLIST, "https://hh.ru/1", "Notes"
        );

        mockMvc.perform(post("/api/v1/parser/import")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
