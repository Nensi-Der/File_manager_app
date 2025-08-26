package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.constants.Roles;
import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.dto.AuthenticationRequest;
import dev.al.FileManagerApplication.dto.AuthenticationResponse;
import dev.al.FileManagerApplication.dto.UserRegistrationRequest;
import dev.al.FileManagerApplication.model.UserEntity;
import dev.al.FileManagerApplication.repository.UserRepository;
import dev.al.FileManagerApplication.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Locale;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final MessageSource messageSource;

    public AuthController(MessageSource messageSource, AuthenticationManager authenticationManager, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.messageSource = messageSource;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(
            @RequestBody UserRegistrationRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            String message = messageSource.getMessage("name.exists", null, locale);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(message));
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRoles(Collections.singleton(Roles.USER)); // assuming you have a Roles enum or constants

        userRepository.save(user);

        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @RequestBody AuthenticationRequest request,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            String message = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(message));
        }

        UserEntity user = userRepository.findByUsername(request.getUsername()).get();

        String jwt = jwtUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRoles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                )
        );

        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(new AuthenticationResponse(jwt), message));
    }
}
