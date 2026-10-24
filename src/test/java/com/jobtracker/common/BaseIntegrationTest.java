package com.jobtracker.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.application.repository.ApplicationRepository;
import com.jobtracker.security.jwt.JwtService;
import com.jobtracker.user.dto.AuthResponse;
import com.jobtracker.user.dto.RegisterRequest;
import com.jobtracker.user.repository.UserRepository;
import com.jobtracker.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("jobtracker_test")
                    .withUsername("jobtracker_test")
                    .withPassword("jobtracker_test");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ApplicationRepository applicationRepository;

    @Autowired
    protected AuthService authService;

    @Autowired
    protected JwtService jwtService;

    @BeforeEach
    void clearDatabase() {
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected AuthResponse createAndAuthenticateUser(String email, String fullName, String password) {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        return authService.register(request);
    }
}
