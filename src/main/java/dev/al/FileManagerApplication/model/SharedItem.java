package dev.al.FileManagerApplication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Entity
public class SharedItem {
    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @Setter
    @Getter
    private UserEntity sharedWith;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private PermissionType permission;

    @ManyToOne
    @Setter
    @Getter
    private FileEntity file;

    @ManyToOne
    @Setter
    @Getter
    private FolderEntity folder;

    // Getters, Setters
}