package com.jobtracker.parser.service;

import com.jobtracker.parser.dto.ParsedVacancyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeadHunterParserService {

    private static final Logger log = LoggerFactory.getLogger(HeadHunterParserService.class);
    private static final Pattern COMPANY_PATTERN = Pattern.compile("Вакансия компании:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SALARY_PATTERN = Pattern.compile("Предполагаемый уровень месячного дохода:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_PATTERN = Pattern.compile("Регион:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    public HeadHunterParserService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ParsedVacancyDto> searchVacancies(String keyword, int limit) {
        List<ParsedVacancyDto> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return result;
        }

        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = "https://hh.ru/search/vacancy/rss?text=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("HeadHunter RSS returned HTTP status {}", response.statusCode());
                return result;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Secure XML parser configuration against XXE
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(response.body());
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            int maxItems = Math.min(items.getLength(), limit > 0 ? limit : 20);

            for (int i = 0; i < maxItems; i++) {
                Element item = (Element) items.item(i);
                String title = getTagValue(item, "title");
                String link = getTagValue(item, "link");
                String pubDateStr = getTagValue(item, "pubDate");
                String description = getTagValue(item, "description");

                String company = "Компания с hh.ru";
                StringBuilder notesBuilder = new StringBuilder();

                if (description != null) {
                    Matcher companyMatcher = COMPANY_PATTERN.matcher(description);
                    if (companyMatcher.find()) {
                        company = companyMatcher.group(1).trim();
                    }

                    Matcher regionMatcher = REGION_PATTERN.matcher(description);
                    if (regionMatcher.find()) {
                        notesBuilder.append("Город: ").append(regionMatcher.group(1).trim()).append(". ");
                    }

                    Matcher salaryMatcher = SALARY_PATTERN.matcher(description);
                    if (salaryMatcher.find()) {
                        String sal = salaryMatcher.group(1).trim();
                        if (!sal.equalsIgnoreCase("не указан")) {
                            notesBuilder.append("Доход: ").append(sal).append(". ");
                        }
                    }
                }

                notesBuilder.append("Импортировано из HeadHunter (по запросу '").append(keyword.trim()).append("').");

                LocalDate appliedAt = LocalDate.now();
                if (pubDateStr != null && !pubDateStr.isEmpty()) {
                    try {
                        appliedAt = OffsetDateTime.parse(pubDateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate();
                    } catch (Exception ignored) {
                    }
                }

                if (company.length() > 120) {
                    company = company.substring(0, 117) + "...";
                }
                if (title != null && title.length() > 120) {
                    title = title.substring(0, 117) + "...";
                }

                String notes = notesBuilder.toString();
                if (notes.length() > 2000) {
                    notes = notes.substring(0, 1997) + "...";
                }

                result.add(new ParsedVacancyDto(
                        company,
                        title != null ? title : "Разработчик",
                        link,
                        notes,
                        appliedAt,
                        "HH"
                ));
            }
        } catch (Exception e) {
            log.error("Error parsing HeadHunter vacancies for query '{}': {}", keyword, e.getMessage(), e);
        }

        return result;
    }

    private String getTagValue(Element element, String tagName) {
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return null;
    }
}
