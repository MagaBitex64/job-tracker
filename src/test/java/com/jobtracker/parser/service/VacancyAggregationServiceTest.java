package com.jobtracker.parser.service;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.parser.dto.ImportVacancyRequest;
import com.jobtracker.parser.dto.ParsedVacancyDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacancyAggregationServiceTest {

    @Mock
    private HeadHunterParserService headHunterParserService;

    @Mock
    private LinkedInParserService linkedInParserService;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private VacancyAggregationService aggregationService;

    @Test
    @DisplayName("searchAcrossPlatforms returns empty list when query is empty")
    void searchAcrossPlatforms_emptyQuery() {
        List<ParsedVacancyDto> result = aggregationService.searchAcrossPlatforms("   ", "ALL", 10);
        assertThat(result).isEmpty();
        verifyNoInteractions(headHunterParserService, linkedInParserService);
    }

    @Test
    @DisplayName("searchAcrossPlatforms aggregates both HH and LinkedIn for ALL")
    void searchAcrossPlatforms_allSources() {
        ParsedVacancyDto hhDto = new ParsedVacancyDto("Yandex", "Java Dev", "https://hh.ru/1", "Good", LocalDate.now(), "HeadHunter");
        ParsedVacancyDto liDto = new ParsedVacancyDto("Google", "Backend Engineer", "https://linkedin.com/1", "Great", LocalDate.now(), "LinkedIn");

        when(headHunterParserService.searchVacancies(eq("Java"), eq("any"), any(), eq(false), eq(10)))
                .thenReturn(List.of(hhDto));
        when(linkedInParserService.searchVacancies(eq("Java"), eq("any"), any(), eq(false), eq(10)))
                .thenReturn(List.of(liDto));

        List<ParsedVacancyDto> result = aggregationService.searchAcrossPlatforms("Java", "ALL", 10);

        assertThat(result).hasSize(2).containsExactly(hhDto, liDto);
        verify(headHunterParserService).searchVacancies(eq("Java"), eq("any"), any(), eq(false), eq(10));
        verify(linkedInParserService).searchVacancies(eq("Java"), eq("any"), any(), eq(false), eq(10));
    }

    @Test
    @DisplayName("searchAcrossPlatforms passes experience, location and remoteOnly parameters properly")
    void searchAcrossPlatforms_withFilters() {
        ParsedVacancyDto hhDto = new ParsedVacancyDto(
                "Kaspi.kz", "Junior Java Developer", "https://hh.ru/3", "Notes", LocalDate.now(),
                "HH", "Алматы", "от 300 000 ₸", "Без опыта"
        );

        when(headHunterParserService.searchVacancies(eq("Java"), eq("noExperience"), eq("Алматы"), eq(true), eq(15)))
                .thenReturn(List.of(hhDto));

        List<ParsedVacancyDto> result = aggregationService.searchAcrossPlatforms(
                "Java", "HH", "noExperience", "Алматы", true, 15
        );

        assertThat(result).hasSize(1).containsExactly(hhDto);
        assertThat(result.get(0).location()).isEqualTo("Алматы");
        assertThat(result.get(0).experience()).isEqualTo("Без опыта");
        verify(headHunterParserService).searchVacancies(eq("Java"), eq("noExperience"), eq("Алматы"), eq(true), eq(15));
        verifyNoInteractions(linkedInParserService);
    }

    @Test
    @DisplayName("searchAcrossPlatforms queries only HH when source is HH")
    void searchAcrossPlatforms_hhOnly() {
        ParsedVacancyDto hhDto = new ParsedVacancyDto("Sber", "Senior Java", "https://hh.ru/2", "Notes", LocalDate.now(), "HeadHunter");

        when(headHunterParserService.searchVacancies(eq("Java"), any(), any(), anyBoolean(), eq(5)))
                .thenReturn(List.of(hhDto));

        List<ParsedVacancyDto> result = aggregationService.searchAcrossPlatforms("Java", "HH", 5);

        assertThat(result).hasSize(1).containsExactly(hhDto);
        verify(headHunterParserService).searchVacancies(eq("Java"), any(), any(), anyBoolean(), eq(5));
        verifyNoInteractions(linkedInParserService);
    }

    @Test
    @DisplayName("importVacancy converts request to CreateApplicationRequest and delegates to ApplicationService")
    void importVacancy_success() {
        Long userId = 101L;
        ImportVacancyRequest request = new ImportVacancyRequest(
                "Tinkoff",
                "Java Software Engineer",
                ApplicationStatus.WISHLIST,
                "https://hh.ru/vacancy/123",
                "Зарплата до 350 000 руб."
        );

        ApplicationResponse expected = new ApplicationResponse(
                1L,
                "Tinkoff",
                "Java Software Engineer",
                ApplicationStatus.WISHLIST,
                "https://hh.ru/vacancy/123",
                "Зарплата до 350 000 руб.",
                LocalDate.now(),
                Instant.now(),
                Instant.now()
        );

        when(applicationService.createApplication(eq(userId), any(CreateApplicationRequest.class))).thenReturn(expected);

        ApplicationResponse actual = aggregationService.importVacancy(userId, request);

        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(1L);
        assertThat(actual.company()).isEqualTo("Tinkoff");

        ArgumentCaptor<CreateApplicationRequest> captor = ArgumentCaptor.forClass(CreateApplicationRequest.class);
        verify(applicationService).createApplication(eq(userId), captor.capture());

        CreateApplicationRequest captured = captor.getValue();
        assertThat(captured.company()).isEqualTo("Tinkoff");
        assertThat(captured.position()).isEqualTo("Java Software Engineer");
        assertThat(captured.status()).isEqualTo(ApplicationStatus.WISHLIST);
        assertThat(captured.link()).isEqualTo("https://hh.ru/vacancy/123");
        assertThat(captured.notes()).isEqualTo("Зарплата до 350 000 руб.");
        assertThat(captured.appliedAt()).isEqualTo(LocalDate.now());
    }
}
