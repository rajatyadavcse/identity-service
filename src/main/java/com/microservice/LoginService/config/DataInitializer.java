package com.microservice.LoginService.config;

import com.microservice.LoginService.entity.Role;
import com.microservice.LoginService.entity.User;
import com.microservice.LoginService.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Slf4j
@Configuration
public class DataInitializer {

    private static final String SUPER_USER     = "super_user";
    private static final String SUPER_PASSWORD = "password";   // plain-text; encoded below

    @Bean
    public CommandLineRunner seedSuperUser(UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            Optional<User> existing = userRepository.findByUsername(SUPER_USER);

            if (existing.isEmpty()) {
                // First-time seed: create the user with a hashed password
                User user = User.builder()
                        .username(SUPER_USER)
                        .password(passwordEncoder.encode(SUPER_PASSWORD))
                        .role(Role.SUPER_ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(user);
                log.info("DataInitializer: created '{}' with role SUPER_ADMIN", SUPER_USER);

            } else {
                // User exists — verify the stored hash actually matches SUPER_PASSWORD.
                // A prefix check ($2a$) is not enough: the hash may have been manually set
                // to a hash of a DIFFERENT string, which would still look like BCrypt.
                User user = existing.get();
                boolean passwordCorrect = passwordEncoder.matches(SUPER_PASSWORD, user.getPassword());
                if (!passwordCorrect) {
                    user.setPassword(passwordEncoder.encode(SUPER_PASSWORD));
                    userRepository.save(user);
                    log.warn("DataInitializer: stored password for '{}' did not match — re-encoded to BCrypt", SUPER_USER);
                } else {
                    log.info("DataInitializer: '{}' password verified OK — no changes needed", SUPER_USER);
                }
            }
        };
    }
}
