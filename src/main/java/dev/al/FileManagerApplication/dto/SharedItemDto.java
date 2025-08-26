package dev.al.FileManagerApplication.dto;

import dev.al.FileManagerApplication.model.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SharedItemDto {
    private Long id;
    private String sharedWith;
    private PermissionType permission;
    private Long fileId;
    private Long folderId;
}