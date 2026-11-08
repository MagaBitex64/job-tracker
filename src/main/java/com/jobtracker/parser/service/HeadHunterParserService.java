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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeadHunterParserService {

    private static final Logger log = LoggerFactory.getLogger(HeadHunterParserService.class);
    private static final Pattern COMPANY_PATTERN = Pattern.compile("Вакансия компании:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SALARY_PATTERN = Pattern.compile("Предполагаемый уровень месячного дохода:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_PATTERN = Pattern.compile("Регион:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE);

    // Map common country and city names to HeadHunter area codes
    private static final Map<String, Integer> HH_AREA_MAP;
    static {
        Map<String, Integer> map = new HashMap<>();
        // Countries
        map.put("россия", 113);
        map.put("russia", 113);
        map.put("казахстан", 40);
        map.put("kazakhstan", 40);
        map.put("беларусь", 16);
        map.put("belarus", 16);
        map.put("узбекистан", 97);
        map.put("uzbekistan", 97);
        map.put("кыргызстан", 48);
        map.put("киргизия", 48);
        map.put("грузия", 28);
        map.put("georgia", 28);
        map.put("армения", 9);
        map.put("armenia", 9);
        map.put("азербайджан", 5);

        // Major Cities
        map.put("москва", 1);
        map.put("moscow", 1);
        map.put("санкт-петербург", 2);
        map.put("питер", 2);
        map.put("спб", 2);
        map.put("saint petersburg", 2);
        map.put("st petersburg", 2);
        map.put("алматы", 160);
        map.put("almaty", 160);
        map.put("астана", 159);
        map.put("astana", 159);
        map.put("нур-султан", 159);
        map.put("шымкент", 209);
        map.put("shymkent", 209);
        map.put("караганда", 165);
        map.put("атырау", 163);
        map.put("актобе", 162);
        map.put("минск", 1002);
        map.put("minsk", 1002);
        map.put("ташкент", 2759);
        map.put("tashkent", 2759);
        map.put("бишкек", 202);
        map.put("bishkek", 202);
        map.put("тбилиси", 2154);
        map.put("tbilisi", 2154);
        map.put("ереван", 1238);
        map.put("новосибирск", 4);
        map.put("екатеринбург", 3);
        map.put("казань", 88);
        map.put("нижний новгород", 66);
        map.put("самара", 78);
        map.put("ростов-на-дону", 76);
        map.put("краснодар", 53);
        map.put("воронеж", 26);
        HH_AREA_MAP = Collections.unmodifiableMap(map);
    }

    private final HttpClient httpClient;

    public HeadHunterParserService() {
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
            StringBuilder urlBuilder = new StringBuilder("https://hh.ru/search/vacancy/rss?text=");
            String queryText = keyword.trim();

            Integer areaId = null;
            if (location != null && !location.trim().isEmpty()) {
                String locClean = location.trim().toLowerCase();
                areaId = HH_AREA_MAP.get(locClean);
                if (areaId == null) {
                    // Not a predefined area id, append to text search
                    queryText = queryText + " " + location.trim();
                }
            }

            urlBuilder.append(URLEncoder.encode(queryText, StandardCharsets.UTF_8));

            if (areaId != null) {
                urlBuilder.append("&area=").append(areaId);
            }

            if (experience != null && !experience.trim().isEmpty() && !"any".equalsIgnoreCase(experience.trim())) {
                urlBuilder.append("&experience=").append(URLEncoder.encode(experience.trim(), StandardCharsets.UTF_8));
            }

            if (remoteOnly) {
                urlBuilder.append("&schedule=remote");
            }

            String url = urlBuilder.toString();
            log.info("Requesting HeadHunter RSS: {}", url);

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
                String parsedLocation = null;
                String parsedSalary = null;
                StringBuilder notesBuilder = new StringBuilder();

                if (description != null) {
                    Matcher companyMatcher = COMPANY_PATTERN.matcher(description);
                    if (companyMatcher.find()) {
                        company = companyMatcher.group(1).trim();
                    }

                    Matcher regionMatcher = REGION_PATTERN.matcher(description);
                    if (regionMatcher.find()) {
                        parsedLocation = regionMatcher.group(1).trim();
                        notesBuilder.append("Город: ").append(parsedLocation).append(". ");
                    }

                    Matcher salaryMatcher = SALARY_PATTERN.matcher(description);
                    if (salaryMatcher.find()) {
                        String sal = salaryMatcher.group(1).trim();
                        if (!sal.equalsIgnoreCase("не указан")) {
                            parsedSalary = sal;
                            notesBuilder.append("Доход: ").append(sal).append(". ");
                        }
                    }
                }

                String expLabel = formatExperienceLabel(experience);
                if (expLabel != null) {
                    notesBuilder.append("Требуемый опыт: ").append(expLabel).append(". ");
                }

                if (remoteOnly) {
                    notesBuilder.append("Формат: Удаленная работа. ");
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
                        "HH",
                        parsedLocation,
                        parsedSalary,
                        expLabel
                ));
            }
        } catch (Exception e) {
            log.error("Error parsing HeadHunter vacancies for query '{}': {}", keyword, e.getMessage(), e);
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

    private String getTagValue(Element element, String tagName) {
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return null;
    }
}
