package com.visage.cloudstorage.unit.Services;


import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Repositories.UserRepository;
import com.visage.cloudstorage.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp(){
        userDetailsService = userService.getUsername();
        assertNotNull(userDetailsService,"Не должен быть пустым");
    }

    @Test
    void shouldReturnUserDetailsWhenUserExists(){
        String username = "Username";
        User user = mock(User.class);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        assertNotNull(userDetails,"Не должен быть null");
        assertSame(user,userDetails," Ожидаемый объект юзер");
    }
    @Test
    void shouldBeReturnUserDetailsWhenUserNotExist(){
        String username = "Username";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username),"Выбрашена ошибка");
        assertEquals("User Not Found",exception.getMessage(), " Проверка совпадает ли сигнатура ошибки");
        verify(userRepository,times(1)).findByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }

}