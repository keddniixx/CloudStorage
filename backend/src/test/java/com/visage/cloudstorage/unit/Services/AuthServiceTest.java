package com.visage.cloudstorage.unit.Services;

import com.visage.cloudstorage.Model.AuthReqest;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.AuthService;
import com.visage.cloudstorage.Services.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock ResourceService resourceService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, resourceService);
    }

    @Test
    void rejectsInvalidCredentials() {
        AuthReqest request = new AuthReqest();
        request.setUsername("account");
        request.setPassword("invalid");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));
        assertThrows(BadCredentialsException.class, () -> authService.auth(request));
    }
}
