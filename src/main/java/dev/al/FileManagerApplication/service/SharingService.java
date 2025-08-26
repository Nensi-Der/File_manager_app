package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.dto.ShareRequestDto;
import dev.al.FileManagerApplication.dto.SharedItemDto;
import dev.al.FileManagerApplication.model.PermissionType;

import java.util.List;

public interface SharingService {
    void shareItem(ShareRequestDto dto);
    List<SharedItemDto> getSharedItemsForUser(String username);
    boolean hasPermission(String username, Long fileId, Long folderId, PermissionType requiredPermission);
}