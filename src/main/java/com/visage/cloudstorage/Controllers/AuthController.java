package com.visage.cloudstorage.Controllers;


import com.visage.cloudstorage.Model.*;
import com.visage.cloudstorage.Services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/auth/sign-up")
    public ResponseEntity<?> register(@RequestBody RegisterReqest reqest) {
        if (reqest.getUsername().length() < 5 || reqest.getUsername().length() > 40){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder().status(400).message("Ошибка валидации имени: длина должна быть от 5 до 40 символов").build());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(reqest));
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<?> login(@RequestBody AuthReqest reqest){
        if (reqest.getUsername().length() < 5 || reqest.getUsername().length() > 40){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder().status(400).message("Ошибка валидации имени: длина должна быть от 5 до 40 символов").build());
        }
        return ResponseEntity.ok().body(service.auth(reqest));
    }

    @GetMapping("/user/me")
    public ResponseEntity<?> pingSession(@AuthenticationPrincipal UserDetails userDetails){
        if (userDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder().status(401).message("Пользоватль не авторизован").build());
        }
        UserResponse userResponse = new UserResponse(userDetails.getUsername());
        return ResponseEntity.ok().body(userResponse);
    }
}