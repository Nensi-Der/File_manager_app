package dev.al.FileManagerApplication.dto;

import dev.al.FileManagerApplication.model.PermissionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestDto {
    private Long fileId;
    private Long folderId;
    private PermissionType requestedPermission;
}