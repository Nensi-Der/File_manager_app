package dev.al.FileManagerApplication.dto;

import dev.al.FileManagerApplication.model.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ShareRequestDto {
    private Long fileId; // nullable
    private Long folderId; // nullable
    private String username;
    private PermissionType permission;
}