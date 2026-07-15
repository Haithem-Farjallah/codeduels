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
import io.jsonwebtoken.JwtException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private  AccountRepository accountRepository;
    private  RoleRepository roleRepository;
    private  PasswordEncoder passwordEncoder;
    private  AuthenticationManager authenticationManager;
    private  JwtService jwtService;

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
        accountRepository.save(newAccount);

        //TODO: link to user model the display name and account id
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


}
