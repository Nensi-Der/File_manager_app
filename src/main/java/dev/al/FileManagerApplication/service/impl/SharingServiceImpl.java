package dev.al.FileManagerApplication.service.impl;

import dev.al.FileManagerApplication.dto.ShareRequestDto;
import dev.al.FileManagerApplication.dto.SharedItemDto;
import dev.al.FileManagerApplication.model.*;
import dev.al.FileManagerApplication.service.SharingService;
import dev.al.FileManagerApplication.repository.FileRepository;
import dev.al.FileManagerApplication.repository.FolderRepository;
import dev.al.FileManagerApplication.repository.SharedItemRepository;
import dev.al.FileManagerApplication.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SharingServiceImpl implements SharingService {

    private final SharedItemRepository sharedItemRepo;
    private final UserRepository userRepo;
    private final FileRepository fileRepo;
    private final FolderRepository folderRepo;
    private final MessageSource messageSource;

    public SharingServiceImpl(SharedItemRepository sharedItemRepo,
                              UserRepository userRepo,
                              FileRepository fileRepo,
                              FolderRepository folderRepo,
                              MessageSource messageSource) {
        this.sharedItemRepo = sharedItemRepo;
        this.userRepo = userRepo;
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.messageSource = messageSource;
    }

    @Override
    public void shareItem(ShareRequestDto dto) {
        UserEntity user = userRepo.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("user.not.found", new Object[]{dto.getUsername()}, Locale.getDefault())
                ));

        SharedItem sharedItem = new SharedItem();
        sharedItem.setSharedWith(user);
        sharedItem.setPermission(dto.getPermission());

        if (dto.getFileId() != null) {
            FileEntity file = fileRepo.findById(dto.getFileId())
                    .orElseThrow(() -> new RuntimeException(
                            messageSource.getMessage("file.not.found", new Object[]{dto.getFileId()}, Locale.getDefault())
                    ));
            sharedItem.setFile(file);
        } else if (dto.getFolderId() != null) {
            FolderEntity folder = folderRepo.findById(dto.getFolderId())
                    .orElseThrow(() -> new RuntimeException(
                            messageSource.getMessage("folder.not.found", new Object[]{dto.getFolderId()}, Locale.getDefault())
                    ));
            sharedItem.setFolder(folder);
        }

        sharedItemRepo.save(sharedItem);
    }

    @Override
    public List<SharedItemDto> getSharedItemsForUser(String username) {
        return sharedItemRepo.findBySharedWithUsername(username).stream().map(shared -> {
            SharedItemDto dto = new SharedItemDto();
            dto.setId(shared.getId());
            dto.setSharedWith(shared.getSharedWith().getUsername());
            dto.setPermission(shared.getPermission());
            if (shared.getFile() != null) dto.setFileId(shared.getFile().getId());
            if (shared.getFolder() != null) dto.setFolderId(shared.getFolder().getId());
            return dto;
        }).toList();
    }

    @Override
    public boolean hasPermission(String username, Long fileId, Long folderId, PermissionType permission) {
        List<SharedItem> sharedItems;

        if (fileId != null) {
            sharedItems = sharedItemRepo.findBySharedWithUsername(username).stream()
                    .filter(si -> si.getFile() != null && si.getFile().getId().equals(fileId))
                    .toList();
        } else if (folderId != null) {
            sharedItems = sharedItemRepo.findBySharedWithUsername(username).stream()
                    .filter(si -> si.getFolder() != null && si.getFolder().getId().equals(folderId))
                    .toList();
        } else {
            return false; // no target specified
        }

        return sharedItems.stream()
                .anyMatch(si -> {
                    PermissionType p = si.getPermission();
                    if (permission == PermissionType.READ) {
                        return p == PermissionType.READ || p == PermissionType.WRITE;
                    }
                    return p == permission;
                });
    }
}
