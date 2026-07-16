package com.visage.cloudstorage.Exceptions;

import com.visage.cloudstorage.Model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                .status(500)
                .message("Неизвестная ошибка")
                .build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtime(){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .status(500)
                .message("Неизвестная ошибка")
                .build()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> userAlreadyExists(){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .status(409)
                .message("Пользователь с таким именем уже существует")
                .build()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badUsernameOrPassword(){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.builder()
                .status(401)
                .message("Введены неверные данные")
                .build());
    }


    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<?> dataNotFoundException(DataNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .status(404)
                .message(e.getMessage())
                .build());
    }

    @ExceptionHandler(NotCorrectNameFileOrPackage.class)
    public ResponseEntity<?> notCorrectNameFileOrPackage(){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .status(409)
                .message("Такое имя файла уже существует")
                .build());

    }

    @ExceptionHandler(ParentFileIsAlreadyExists.class)
    public ResponseEntity<?> parentNotExists(){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                .status(404)
                .message("Родительской папки не существует")
                .build());
    }


}