package com.codeduels.user.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.user.dto.UserResponse;
import com.codeduels.user.model.User;
import com.codeduels.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<UserResponse>> me() {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RessourceNotFoundException("User not found"));
        return ResponseEntity.ok(StandardResponse.success(
                new UserResponse(user.getId(), user.getUsername(), user.getAvatarUrl())));
    }
}
