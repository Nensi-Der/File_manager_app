package dev.al.FileManagerApplication.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegistrationRequest {
    // Getters and setters
    private String username;
    private String password;
    private String email;

}