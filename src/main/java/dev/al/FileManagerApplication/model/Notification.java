package dev.al.FileManagerApplication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Notification {
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
@Setter
@Getter
    private String message;
@Setter
@Getter
    private boolean read = false;
@Setter
@Getter
    private LocalDateTime createdAt = LocalDateTime.now();
@Setter
@Getter
    @ManyToOne
    private UserEntity recipient;


}