package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.model.UserEntity;
import dev.al.FileManagerApplication.repository.UserRepository;
import dev.al.FileManagerApplication.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register new user with role USER by default
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserEntity>> registerUser(@RequestBody UserEntity user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("user.registration.username.taken"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Collections.singleton("ROLE_USER")); // default role
        UserEntity savedUser = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(savedUser, "user.registration.success"));
    }
}