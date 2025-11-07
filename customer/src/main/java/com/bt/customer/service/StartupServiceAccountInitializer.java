package com.bt.customer.service;

import com.bt.customer.config.ServiceAccountProperties;
import com.bt.customer.entity.User;
import com.bt.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupServiceAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceAccountProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.getEmail();
        String password = properties.getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Service account credentials are not configured; skipping provisioning");
            return;
        }
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing == null) {
            User created = User.builder()
                    .email(email)
                    .username(email)
                    .fullName("Accounts Service User")
                    .password(passwordEncoder.encode(password))
                    .role(properties.getRole())
                    .active(true)
                    .twoFactorEnabled(false)
                    .build();
            userRepository.save(created);
            log.info("Provisioned service account {}", email);
            return;
        }
        boolean changed = false;
        if (!passwordEncoder.matches(password, existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(password));
            changed = true;
        }
        if (existing.getRole() != properties.getRole()) {
            existing.setRole(properties.getRole());
            changed = true;
        }
        if (existing.getActive() == null || !existing.getActive()) {
            existing.setActive(true);
            changed = true;
        }
        if (existing.getTwoFactorEnabled() == null || existing.getTwoFactorEnabled()) {
            existing.setTwoFactorEnabled(false);
            changed = true;
        }
        if (changed) {
            userRepository.save(existing);
            log.info("Updated service account {}", email);
        }
    }
}
