package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Model.UserResponse;
import com.visage.cloudstorage.Model.*;
import com.visage.cloudstorage.Repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ResourceService resourceService;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, ResourceService resourceService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.resourceService = resourceService;
    }

    public UserResponse register(RegisterReqest reqest) {
        User user = User.builder()
                .username(reqest.getUsername())
                .password(passwordEncoder.encode(reqest.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);
        resourceService.createTheCentralPoint(user.getId());
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            reqest.getUsername(),
                            reqest.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during authentication", e);
        }
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authenticate);
        HttpSession session;
        try {
            session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession(true);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Не удалось получить сессию", e);
        }
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession(false);

        Object principal = authenticate.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Неожиданный тип principal: " + principal.getClass().getName());
        }
        UserResponse userResponse = new UserResponse(user.getUsername());
        return userResponse;
    }

    public UserResponse auth(AuthReqest reqest) {
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            reqest.getUsername(),
                            reqest.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Непредвиденная ошибка при проверке подлинности", e);
        }
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authenticate);
        HttpSession session;
        try {
            session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession(true);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Не удалось получить сессию", e);
        }
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getSession(false);

        Object principal = authenticate.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Неожиданный тип principal: " + principal.getClass().getName());
        }
        User user = (User) principal;
        UserResponse userResponse = new UserResponse(user.getUsername());
        return userResponse;
    }
}