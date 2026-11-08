package com.jobtracker.parser.service;

import com.jobtracker.parser.dto.ParsedVacancyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LinkedInParserService {

    private static final Logger log = LoggerFactory.getLogger(LinkedInParserService.class);

    // Regular expressions for parsing public job guest card HTML snippet
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "<div[^>]*class=\"[^\"]*job-search-card[^\"]*\"[\\s\\S]*?</div>\\s*</li>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<h3[^>]*class=\"[^\"]*base-search-card__title[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</h3>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "<h4[^>]*class=\"[^\"]*base-search-card__subtitle[^\"]*\"[\\s\\S]*?>\\s*<a[^>]*>\\s*([^<]+?)\\s*</a>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMPANY_FALLBACK_PATTERN = Pattern.compile(
            "<h4[^>]*class=\"[^\"]*base-search-card__subtitle[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</h4>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a[^>]*class=\"[^\"]*base-card__full-link[^\"]*\"[^>]*href=\"([^\"]+?)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*job-search-card__location[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "<span[^>]*class=\"[^\"]*job-search-card__salary-info[^\"]*\"[^>]*>\\s*([^<]+?)\\s*</span>",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;

    public LinkedInParserService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ParsedVacancyDto> searchVacancies(String keyword, int limit) {
        return searchVacancies(keyword, "any", null, false, limit);
    }

    public List<ParsedVacancyDto> searchVacancies(
            String keyword,
            String experience,
            String location,
            boolean remoteOnly,
            int limit
    ) {
        List<ParsedVacancyDto> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return result;
        }

        try {
            StringBuilder urlBuilder = new StringBuilder("https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search?keywords=");
            String queryText = keyword.trim();

            // Refine search query for junior / entry level
            if ("noExperience".equalsIgnoreCase(experience)) {
                queryText = queryText + " (Junior OR Intern OR Trainee OR Entry)";
            }

            urlBuilder.append(URLEncoder.encode(queryText, StandardCharsets.UTF_8));

            if (location != null && !location.trim().isEmpty()) {
                urlBuilder.append("&location=").append(URLEncoder.encode(location.trim(), StandardCharsets.UTF_8));
            }

            // LinkedIn Remote Filter
            if (remoteOnly) {
                urlBuilder.append("&f_WT=2");
            }

            // LinkedIn Experience levels:
            // 1 = Internship, 2 = Entry level, 3 = Associate, 4 = Mid-Senior level, 5 = Director, 6 = Executive
            if ("noExperience".equalsIgnoreCase(experience)) {
                urlBuilder.append("&f_E=1%2C2");
            } else if ("between1And3".equalsIgnoreCase(experience)) {
                urlBuilder.append("&f_E=2%2C3");
            } else if ("between3And6".equalsIgnoreCase(experience)) {
                urlBuilder.append("&f_E=4");
            } else if ("moreThan6".equalsIgnoreCase(experience)) {
                urlBuilder.append("&f_E=5%2C6");
            }

            urlBuilder.append("&start=0");

            String url = urlBuilder.toString();
            log.info("Requesting LinkedIn guest jobs: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body() == null) {
                log.warn("LinkedIn guest jobs API returned HTTP status {}", response.statusCode());
                return result;
            }

            String html = response.body();
            Matcher cardMatcher = CARD_PATTERN.matcher(html);
            int maxItems = limit > 0 ? limit : 20;

            while (cardMatcher.find() && result.size() < maxItems) {
                String cardHtml = cardMatcher.group();

                String rawTitle = extract(TITLE_PATTERN, cardHtml, "Developer");
                String title = cleanHtml(rawTitle);

                // Filter out senior / lead positions if user explicitly asked for noExperience
                if ("noExperience".equalsIgnoreCase(experience)) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("senior") || lowerTitle.contains("lead")
                            || lowerTitle.contains("principal") || lowerTitle.contains("architect")
                            || lowerTitle.contains("director") || lowerTitle.contains("head of")) {
                        continue;
                    }
                }

                String company = extract(COMPANY_PATTERN, cardHtml, null);
                if (company == null) {
                    company = extract(COMPANY_FALLBACK_PATTERN, cardHtml, "LinkedIn Employer");
                }
                company = cleanHtml(company);

                String link = extract(LINK_PATTERN, cardHtml, null);
                String parsedLocation = extract(LOCATION_PATTERN, cardHtml, null);
                if (parsedLocation != null) {
                    parsedLocation = cleanHtml(parsedLocation);
                }

                String parsedSalary = extract(SALARY_PATTERN, cardHtml, null);
                if (parsedSalary != null) {
                    parsedSalary = cleanHtml(parsedSalary);
                }

                // Clean link tracking parameters if needed
                if (link != null && link.contains("?")) {
                    link = link.substring(0, link.indexOf("?"));
                }

                String expLabel = formatExperienceLabel(experience);

                StringBuilder notes = new StringBuilder();
                if (parsedLocation != null && !parsedLocation.isEmpty()) {
                    notes.append("Локация: ").append(parsedLocation).append(". ");
                }
                if (parsedSalary != null && !parsedSalary.isEmpty()) {
                    notes.append("Доход: ").append(parsedSalary).append(". ");
                }
                if (expLabel != null) {
                    notes.append("Требуемый опыт: ").append(expLabel).append(". ");
                }
                if (remoteOnly) {
                    notes.append("Формат: Удаленная работа. ");
                }
                notes.append("Импортировано из LinkedIn (по запросу '").append(keyword.trim()).append("').");

                if (company.length() > 120) {
                    company = company.substring(0, 117) + "...";
                }
                if (title.length() > 120) {
                    title = title.substring(0, 117) + "...";
                }
                String finalNotes = notes.toString();
                if (finalNotes.length() > 2000) {
                    finalNotes = finalNotes.substring(0, 1997) + "...";
                }

                result.add(new ParsedVacancyDto(
                        company,
                        title,
                        link,
                        finalNotes,
                        LocalDate.now(),
                        "LINKEDIN",
                        parsedLocation,
                        parsedSalary,
                        expLabel
                ));
            }
        } catch (Exception e) {
            log.error("Error parsing LinkedIn vacancies for query '{}': {}", keyword, e.getMessage(), e);
        }

        return result;
    }

    private String formatExperienceLabel(String exp) {
        if (exp == null || exp.isBlank() || "any".equalsIgnoreCase(exp)) {
            return null;
        }
        return switch (exp.trim()) {
            case "noExperience" -> "Без опыта (Junior / Trainee)";
            case "between1And3" -> "1–3 года (Junior+ / Middle)";
            case "between3And6" -> "3–6 лет (Senior)";
            case "moreThan6" -> "Более 6 лет (Lead)";
            default -> exp;
        };
    }

    private String extract(Pattern pattern, String text, String defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    private String cleanHtml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
    }
}
