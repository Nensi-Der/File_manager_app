package dev.al.FileManagerApplication.service.impl;

import dev.al.FileManagerApplication.dto.AccessRequestDto;
import dev.al.FileManagerApplication.dto.AccessDecisionDto;
import dev.al.FileManagerApplication.model.*;
import dev.al.FileManagerApplication.repository.*;
import dev.al.FileManagerApplication.service.AccessRequestService;
import dev.al.FileManagerApplication.service.NotificationService;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.Locale;
@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    private final AccessRequestRepository accessRequestRepo;
    private final UserRepository userRepo;
    private final FileRepository fileRepo;
    private final FolderRepository folderRepo;
    private final SharedItemRepository sharedItemRepo;
    private final NotificationService notificationService;

    public AccessRequestServiceImpl(AccessRequestRepository accessRequestRepo,
                                    UserRepository userRepo,
                                    FileRepository fileRepo,
                                    FolderRepository folderRepo,
                                    SharedItemRepository sharedItemRepo,
                                    NotificationService notificationService) {
        this.accessRequestRepo = accessRequestRepo;
        this.userRepo = userRepo;
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.sharedItemRepo = sharedItemRepo;
        this.notificationService = notificationService;
    }

    @Override
    public void requestAccess(String requesterUsername, AccessRequestDto dto) {
        UserEntity requester = userRepo.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("user.notfound"));

        AccessRequest request = new AccessRequest();
        request.setRequester(requester);
        request.setRequestedPermission(dto.getRequestedPermission());
        request.setStatus(AccessStatus.PENDING);  // Use enum for status

        if (dto.getFileId() != null) {
            FileEntity file = fileRepo.findById(dto.getFileId())
                    .orElseThrow(() -> new RuntimeException("file.original.notfound"));
            request.setFile(file);
            request.setOwner(file.getOwner());
        } else if (dto.getFolderId() != null) {
            FolderEntity folder = folderRepo.findById(dto.getFolderId())
                    .orElseThrow(() -> new RuntimeException("object.notfound"));
            request.setFolder(folder);
            request.setOwner(folder.getOwner());
        } else {
            throw new RuntimeException("Invalid request: either fileId or folderId must be provided");
        }

        accessRequestRepo.save(request);
        notificationService.notifyUser(request.getOwner().getUsername(), "New access request from " + requesterUsername);
    }

    @Override
    public void handleRequest(String ownerUsername, AccessDecisionDto dto) {
        AccessRequest request = accessRequestRepo.findById(dto.getRequestId())
                .orElseThrow(() -> new RuntimeException("object.notfound"));

        if (!request.getOwner().getUsername().equals(ownerUsername)) {
            throw new RuntimeException("no.permission");
        }

        if (dto.isApproved()) {
            request.setStatus(AccessStatus.APPROVED);
            SharedItem sharedItem = new SharedItem();
            sharedItem.setSharedWith(request.getRequester());
            sharedItem.setPermission(request.getRequestedPermission());

            if (request.getFile() != null) sharedItem.setFile(request.getFile());
            if (request.getFolder() != null) sharedItem.setFolder(request.getFolder());

            sharedItemRepo.save(sharedItem);
            notificationService.notifyUser(request.getRequester().getUsername(), "access.approved");
        } else {
            request.setStatus(AccessStatus.DENIED);
            notificationService.notifyUser(request.getRequester().getUsername(), "access.denied");
        }

        accessRequestRepo.save(request);
    }

    @Override
    public void approveRequest(Long requestId, String ownerUsername, PermissionType permission) {
        // This method seems redundant if handleRequest already handles approval
        throw new UnsupportedOperationException("Use handleRequest to approve or deny requests");
    }
}