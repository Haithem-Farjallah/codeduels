package com.codeduels.auth;

import com.codeduels.auth.dto.RegisterRequest;
import com.codeduels.auth.model.Account;
import com.codeduels.auth.model.Role;
import com.codeduels.auth.model.RoleType;
import com.codeduels.auth.repository.AccountRepository;
import com.codeduels.auth.repository.RoleRepository;
import com.codeduels.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesAccountWithUserRole_whenEmailFree() {
        when(accountRepository.findByEmail("h@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(roleRepository.findByType(RoleType.ROLE_USER))
                .thenReturn(Optional.of(new Role(RoleType.ROLE_USER,"Test Role")));

        authService.register(new RegisterRequest("h@test.com", "password123", "haithem"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
       assertEquals("h@test.com", saved.getEmail());
        assertEquals("$2a$hashed", saved.getPassword());
        assertEquals(1, saved.getRoles().size());
    }
}
