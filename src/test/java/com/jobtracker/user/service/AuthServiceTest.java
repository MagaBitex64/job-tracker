package com.jobtracker.user.service;

import com.jobtracker.common.exception.DuplicateResourceException;
import com.jobtracker.common.exception.UnauthorizedException;
import com.jobtracker.security.UserPrincipal;
import com.jobtracker.security.jwt.JwtService;
import com.jobtracker.user.dto.AuthResponse;
import com.jobtracker.user.dto.LoginRequest;
import com.jobtracker.user.dto.RegisterRequest;
import com.jobtracker.user.dto.UserSummaryResponse;
import com.jobtracker.user.entity.Role;
import com.jobtracker.user.entity.User;
import com.jobtracker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("alice@example.com", "Password123!", "Alice Johnson");
        loginRequest = new LoginRequest("alice@example.com", "Password123!");

        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .password("encoded_password")
                .fullName("Alice Johnson")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("register should succeed when email is unique")
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(1L, "alice@example.com")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("alice@example.com");
        assertThat(response.user().fullName()).isEqualTo("Alice Johnson");
        assertThat(response.user().role()).isEqualTo(Role.ROLE_USER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register should throw DuplicateResourceException when email already exists")
    void shouldThrowDuplicateResourceExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login should succeed with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        UserPrincipal principal = new UserPrincipal(
                1L, "alice@example.com", "encoded_password", "Alice Johnson",
                Collections.singletonList(Role.ROLE_USER::name)
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtService.generateToken(1L, "alice@example.com")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("login should throw UnauthorizedException on invalid credentials")
    void shouldThrowUnauthorizedExceptionOnInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("getCurrentUser should return current authenticated user profile")
    void shouldReturnCurrentUserProfile() {
        UserPrincipal principal = new UserPrincipal(
                1L, "alice@example.com", "encoded_password", "Alice Johnson",
                Collections.singletonList(Role.ROLE_USER::name)
        );

        UserSummaryResponse response = authService.getCurrentUser(principal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.fullName()).isEqualTo("Alice Johnson");
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("getCurrentUser should throw UnauthorizedException when principal is null")
    void shouldThrowUnauthorizedExceptionWhenPrincipalIsNull() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not authenticated");
    }
}
