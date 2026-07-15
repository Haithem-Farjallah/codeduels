package com.codeduels.common.util;

import com.codeduels.auth.model.Role;
import com.codeduels.auth.model.RoleType;
import com.codeduels.auth.repository.AccountRepository;
import com.codeduels.auth.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(AccountRepository accountRepository,
                                   RoleRepository roleRepository) {
        return args -> {
            accountRepository.deleteAll();
            roleRepository.deleteAll();

            Role adminRole = Role.builder()
                        .type(RoleType.ROLE_ADMIN)
                        .description("Administrator that has full access and management permissions")
                        .build();

            Role userRole = Role.builder()
                    .type(RoleType.ROLE_USER)
                    .description("User that access plateform for competition")
                    .build();

            roleRepository.saveAll(List.of(adminRole, userRole));

            log.info("Roles added successfully");

        };
    }

}
