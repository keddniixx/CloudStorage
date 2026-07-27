package com.visage.cloudstorage.integration.Services;


import com.visage.cloudstorage.Model.Role;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.UserService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class UserServiceTest {


    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @MockitoBean
    private MinioClient minioClient;


    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp(){
        userRepository.deleteAll();
    }

    @Autowired
    private UserService userService;

    @Test
    public void getUsernameTest(){
        User user = User.builder()
                .username("Username")
                .password("Password")
                .role(Role.USER)
                .build();
        userRepository.save(user);
        UserDetailsService userDetailsService = userService.getUsername();

        UserDetails loadeduserDetails = assertDoesNotThrow(() -> userDetailsService.loadUserByUsername("Username"));
        assertNotNull(loadeduserDetails.getUsername());
        assertEquals("Username",loadeduserDetails.getUsername());
    }

    @Test
    public void shouldThrowUsernameNotFoundException_WhenUserDoesNotExist(){
        UserDetailsService userDetailsService = userService.getUsername();
        assertThrows(UsernameNotFoundException.class,() -> userDetailsService.loadUserByUsername("Username"));
    }
}