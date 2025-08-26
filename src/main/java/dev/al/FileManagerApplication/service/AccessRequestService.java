package dev.al.FileManagerApplication.service;

import dev.al.FileManagerApplication.dto.AccessRequestDto;
import dev.al.FileManagerApplication.dto.AccessDecisionDto;
import dev.al.FileManagerApplication.model.PermissionType;

public interface AccessRequestService {
    void requestAccess(String requesterUsername, AccessRequestDto dto);
    void handleRequest(String ownerUsername, AccessDecisionDto dto);
    void approveRequest(Long requestId, String ownerUsername, PermissionType permission);
}
