package com.bt.customer.service;

import com.bt.customer.dto.UpdateProfileRequest;
import com.bt.customer.dto.UserProfileResponse;
import com.bt.customer.entity.User;
import com.bt.customer.exception.UserNotFoundException;
import com.bt.customer.repository.UserRepository;
import com.bt.customer.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Cacheable(value = "userProfile", key = "#root.methodName + ':' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return UserProfileResponse.fromUser(user);
    }

    @Cacheable(value = "allCustomers", key = "'all'")
    public List<UserProfileResponse> getAllCustomers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = { "userProfile", "customerById", "allCustomers" }, allEntries = true)
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!user.getEmail().equals(request.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            user.setAddress(request.getAddress());
        }

        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().isBlank()) {
            user.setAadhaarNumber(request.getAadhaarNumber());
        }

        if (request.getPanNumber() != null && !request.getPanNumber().isBlank()) {
            user.setPanNumber(request.getPanNumber());
        }

        if (request.getPreferredCurrency() != null) {
            String code = request.getPreferredCurrency().trim().toUpperCase();
            if (!code.isEmpty()) {
                user.setPreferredCurrency(code);
            }
        }

        User updatedUser = userRepository.save(user);
        return UserProfileResponse.fromUser(updatedUser);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }

    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findByEmail(userPrincipal.getUsername())
                .map(user -> user.getRole().name())
                .orElse("UNKNOWN");
    }

    @Cacheable(value = "customerById", key = "#id")
    public UserProfileResponse getCustomerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer with ID " + id + " not found"));
        return UserProfileResponse.fromUser(user);
    }

    public void enableTwoFactorForCurrentUser() {
        User user = getCurrentUser();
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("Two-factor authentication enabled for user: {}", user.getEmail());
    }

    public void disableTwoFactorForCurrentUser() {
        User user = getCurrentUser();
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        log.info("Two-factor authentication disabled for user: {}", user.getEmail());
    }

    public boolean isTwoFactorEnabledForCurrentUser() {
        User user = getCurrentUser();
        return Boolean.TRUE.equals(user.getTwoFactorEnabled());
    }

    public boolean isTwoFactorEnabled(String email) {
        return userRepository.findByEmail(email)
                .map(user -> Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .orElse(false);
    }

    @CacheEvict(value = "userProfile", allEntries = true)
    public void changePassword(String currentPassword, String newPassword) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
