package com.codeduels.auth.service;

import com.codeduels.auth.dto.LoginRequest;
import com.codeduels.auth.dto.RegisterRequest;
import com.codeduels.auth.model.Account;
import com.codeduels.auth.model.Role;
import com.codeduels.auth.model.RoleType;
import com.codeduels.auth.repository.AccountRepository;
import com.codeduels.auth.repository.RoleRepository;
import com.codeduels.auth.security.UserPrincipal;
import com.codeduels.auth.security.jwt.JwtService;
import com.codeduels.common.exception.InvalidCredentialsException;
import com.codeduels.common.exception.ConflictException;
import com.codeduels.user.model.User;
import com.codeduels.user.repository.UserRepository;
import com.codeduels.user.service.UsernameGenerator;
import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsernameGenerator usernameGenerator;
    private final UserRepository userRepository;

    @Value("${app.avatar.base-url}")
    private String avatarBaseUrl;

    @Transactional
    public void register(RegisterRequest registerRequest) {

        Optional<Account> account = accountRepository.findByEmail(registerRequest.getEmail());
        if(account.isPresent()){
            throw new ConflictException("email already in use");
        }

        String email = registerRequest.getEmail();
        String hashedpassword = passwordEncoder.encode(registerRequest.getPassword());

        Role userRole = roleRepository.findByType(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Roles not seeded"));

        Account newAccount = new Account();
        newAccount.setEmail(email);
        newAccount.setPassword(hashedpassword);
        newAccount.getRoles().add(userRole);

        String username = generateUniqueUsername();
        User user = new User();
        user.setUsername(username);
        user.setAvatarUrl(avatarBaseUrl+username);

        newAccount.setUser(user);
        accountRepository.save(newAccount);
    }

    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        Optional<Account> account = accountRepository.findByEmail(loginRequest.getEmail());
        if(account.isEmpty()){
            throw new InvalidCredentialsException("Email not found");
        }
        Account ac = account.get();
        try{
        Authentication authentication= authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        log.debug(accessToken);
        ac.setRefreshToken(refreshToken);
        return  new LoginResult(accessToken,refreshToken);

        }catch(Exception e){
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        String email;
        try {
            email = jwtService.extractEmail(refreshToken);
        } catch (JwtException e) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (!refreshToken.equals(account.getRefreshToken())) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        UserPrincipal principal = new UserPrincipal(account);
        String newAccess = jwtService.generateAccessToken(principal);
        String newRefresh = jwtService.generateRefreshToken(principal);
        account.setRefreshToken(newRefresh);

        return new LoginResult(newAccess, newRefresh);
    }

    private String generateUniqueUsername() {
        for (int i = 0; i < 10; i++) {
            String candidate = usernameGenerator.generate();
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return usernameGenerator.generate() + "_" + System.currentTimeMillis();
    }
}
