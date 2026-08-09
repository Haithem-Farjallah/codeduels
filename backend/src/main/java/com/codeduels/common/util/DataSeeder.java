package com.codeduels.common.util;

import com.codeduels.auth.model.Account;
import com.codeduels.auth.model.Permission;
import com.codeduels.auth.model.PermissionReference;
import com.codeduels.auth.model.Role;
import com.codeduels.auth.model.RoleType;
import com.codeduels.auth.repository.AccountRepository;
import com.codeduels.auth.repository.PermissionRepository;
import com.codeduels.auth.repository.RoleRepository;
import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import com.codeduels.problem.model.Tag;
import com.codeduels.problem.model.TestCase;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.problem.repository.TagRepository;
import com.codeduels.user.model.User;
import com.codeduels.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@Slf4j
public class DataSeeder {

    @Value("${app.avatar.base-url}")
    private String avatarBaseUrl;

    @Bean
    CommandLineRunner initDatabase(AccountRepository accountRepository,
                                   UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PermissionRepository permissionRepository,
                                   TagRepository tagRepository,
                                   ProblemRepository problemRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            accountRepository.deleteAll();
            userRepository.deleteAll();
            roleRepository.deleteAll();
            permissionRepository.deleteAll();
            problemRepository.deleteAll();
            tagRepository.deleteAll();

            Set<PermissionReference> userRefs= Set.of(
                    PermissionReference.READ_PROBLEM
            );
            Set<Permission> adminPermissions = new HashSet<>();
            Set<Permission> usersPermissions = new HashSet<>();

            for(PermissionReference pr : PermissionReference.values()) {
                Permission permission = new Permission(pr, pr.getDescription());
                adminPermissions.add(permission);
                if(userRefs.contains(pr)) {
                    usersPermissions.add(permission);
                }
            }
            permissionRepository.saveAll(adminPermissions); // adminPermissions contain all the permissions that we defined

            Role adminRole = Role.builder()
                        .type(RoleType.ROLE_ADMIN)
                        .description("Administrator that has full access and management permissions")
                        .permissions(adminPermissions)
                        .build();

            Role userRole = Role.builder()
                    .type(RoleType.ROLE_USER)
                    .description("User that access plateform for competition")
                    .permissions(usersPermissions)
                    .build();

            roleRepository.saveAll(List.of(adminRole, userRole));

            log.info("Roles added successfully");

            User admin =User.builder()
                    .username("admin")
                    .avatarUrl(avatarBaseUrl+"admin")
                    .build();

            Account adminAccount = Account.builder()
                    .email("admin@codeduels.com")
                    .password(passwordEncoder.encode("admin"))
                    .roles(Set.of(adminRole))
                    .user(admin)
                    .build();
            accountRepository.save(adminAccount);
            log.info("Accounts added successfully");

            Tag arrays = new Tag("arrays");
            Tag math = new Tag("math");
            Tag strings = new Tag("strings");
            Tag dp = new Tag("dp");

            List<Problem> problems = List.of(
                    Problem.builder()
                            .status(ProblemStatus.PUBLISHED)
                            .slug("two-sum")
                            .title("Two Sum")
                            .description("Given an array of integers and a target, return indices of two numbers adding to target.")
                            .constraints("2 <= n <= 10^4")
                            .difficulty(Difficulty.EASY)
                            .points(100)
                            .sampleTestCases(List.of(new TestCase("2 7 11 15\n9", "0 1", "2 + 7 = 9")))
                            .tags(Set.of(arrays))
                    .build(),

                    Problem.builder()
                             .slug("fizz-buzz")
                             .title("Fizz Buzz")
                             .description("Print numbers 1..n, replacing multiples of 3 with Fizz, 5 with Buzz, both with FizzBuzz.")
                             .constraints("1 <= n <= 10^4")
                             .difficulty(Difficulty.EASY)
                             .points(100)
                             .status(ProblemStatus.PUBLISHED)
                             .sampleTestCases(List.of(new TestCase("5", "1 2 Fizz 4 Buzz", null)))
                             .tags( Set.of(math))
                    .build(),

                    Problem.builder()
                            .slug("secret-draft")
                            .title("Unfinished Problem")
                            .description("This statement is not done yet.")
                            .constraints(null)
                            .difficulty(Difficulty.HARD)
                            .points(500)
                            .status(ProblemStatus.DRAFT)
                            .sampleTestCases(List.of(new TestCase("x", "y", null)))
                            .tags(Set.of(strings,dp))
                    .build()
            );
            problemRepository.saveAll(problems);

        };
    }

}
