package com.visage.cloudstorage.integration.Controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.visage.cloudstorage.Configurations.SecurityConfig;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {



    @Container
    @ServiceConnection
    static RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:latest"));

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityConfig securityConfig;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldBeRegisterAndCorrectSaveUserInDB() throws Exception {
        RegisterReqest registerReqest = RegisterReqest.builder()
                .password("Password")
                .username("Username")
                .build();
        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registerReqest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Username")).andReturn();
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertEquals("Username", user.getUsername());
        assertNotEquals("Password", user.getPassword());
    }

    @Test
    void shouldBeRegisterAndCorrectSaveUserInDBAndNotCorrectUsername() throws Exception {
        User userToLogin = User.builder()
                .username("Min")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Min");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isBadRequest());
        Optional<User> userOptional = userRepository.findByUsername("Min");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeLoginAndCorrectUsernameAndPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Username")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Username"));
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
        User user = userOptional.get();
        assertEquals("Username", user.getUsername());
        assertNotEquals("Password", user.getPassword());
    }

    @Test
    void shouldBeLoginAndCorrectLoginAndNotCorrectPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Username")
                .password("Some password")
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isUnauthorized());
        Optional<User> userOptional = userRepository.findByUsername("Username");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeLoginAndNotCorrectLoginAndCorrectPassword() throws Exception {
        User userToLogin = User.builder()
                .username("Some username")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Username");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isUnauthorized());
        Optional<User> userOptional = userRepository.findByUsername("Some username");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeLoginAndNotCorrectAuth() throws Exception {
        User userToLogin = User.builder()
                .username("Min")
                .password(passwordEncoder.encode("Password"))
                .role(Role.USER)
                .build();
        userRepository.save(userToLogin);

        AuthReqest authReqest = new AuthReqest();
        authReqest.setUsername("Min");
        authReqest.setPassword("Password");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(authReqest)))
                .andExpect(status().isBadRequest());
        Optional<User> userOptional = userRepository.findByUsername("Min");
        assertTrue(userOptional.isPresent());
    }

    @Test
    void shouldBeOutIsCorrect() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out")
                        .with(user(User.builder().username("Username").role(Role.USER).id(1).password("Password").build()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Void.class)))
                .andExpect(status().isNoContent());
    }

}