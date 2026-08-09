package com.codeduels.auth.security;

import com.codeduels.auth.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Account account;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return account.getRoles().stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(role.getType().name()),
                        role.getPermissions().stream()
                                .map(p -> p.getPermissionReference().name())))
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    public UUID getId() {
        return account.getUser().getId();
    }





}
