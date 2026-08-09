package com.codeduels.common.security;

import com.codeduels.auth.model.PermissionReference;
import com.codeduels.auth.security.UserPrincipal;
import com.codeduels.common.exception.UnauthorisedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUser {

    public UUID getId() {
        return principal()
                .orElseThrow(() -> new UnauthorisedException("No authenticated user"))
                .getId();
    }

    public boolean hasPermission(PermissionReference ref) {
        return principal()
                .map(p -> p.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(ref.name())))
                .orElse(false);
    }

    private Optional<UserPrincipal> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return Optional.of(up);
        }
        return Optional.empty();
    }
}
