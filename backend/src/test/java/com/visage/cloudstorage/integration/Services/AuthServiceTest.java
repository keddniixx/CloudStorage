package com.visage.cloudstorage.integration.Services;

import com.visage.cloudstorage.Model.UserResponse;
import com.visage.cloudstorage.Model.AuthReqest;
import com.visage.cloudstorage.Model.RegisterReqest;
import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.AuthService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.Assert.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class AuthServiceTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");


    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    public void shouldBeRegister() {
        RegisterReqest registerReqest = RegisterReqest.builder()
                .username("username")
                .password("password")
                .build();
        UserResponse userResponse = authService.register(registerReqest);
        assertNotNull(userResponse.username(),"Юзернейм не нулл");
        Optional<User> optionalExpectedUser = userRepository.findByUsername(registerReqest.getUsername());
        assertTrue(optionalExpectedUser.isPresent());
        User expectedUser = optionalExpectedUser.get();
        assertEquals(expectedUser.getUsername(), userResponse.username());
        assertEquals(Role.USER, expectedUser.getRole());
        assertNotEquals(registerReqest.getPassword(), expectedUser.getPassword());
        assertTrue(passwordEncoder.matches(registerReqest.getPassword(), expectedUser.getPassword()));
    }

    @Test
    void shouldBeLoginAndNotRegister(){
        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Some user");
        authReqest.setPassword("Some pass");
        assertThrows(BadCredentialsException.class, () -> authService.auth(authReqest));
    }

    @Test
    void shouldBeLoginAndNotAuthRequest(){
        assertThrows(RuntimeException.class , () -> authService.auth(null));
    }

    @Test
    void shouldBeLoginAndRegister() {
        RegisterReqest register = RegisterReqest.builder()
                .username("username")
                .password("password")
                .build();
        UserResponse userResponse = authService.register(register);
        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("username");
        authReqest.setPassword("password");
        assertNotNull(userResponse.username(),"Юзернейм не нулл");
        Optional<User> optionalExpectedUser = userRepository.findByUsername(userResponse.username());
        assertTrue(optionalExpectedUser.isPresent());
        User expectedUser = optionalExpectedUser.get();
        assertEquals(expectedUser.getUsername(), userResponse.username());
        assertEquals(Role.USER, expectedUser.getRole());
        assertNotEquals(userResponse.username(), expectedUser.getPassword());
    }

    @Test
    void shouldNotRegisterUserWithExistingUsername() {
        RegisterReqest registerReqest = RegisterReqest.builder()
                .username("duplicateUser")
                .password("password1")
                .build();
        authService.register(registerReqest);
        RegisterReqest duplicateReqest = RegisterReqest.builder()
                .username("duplicateUser")
                .password("password2")
                .build();
        assertThrows(Exception.class, () -> authService.register(duplicateReqest));
    }
}