package dev.al.FileManagerApplication.controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import dev.al.FileManagerApplication.model.FolderEntity;
import dev.al.FileManagerApplication.service.FolderService;
import dev.al.FileManagerApplication.dto.ApiResponse;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.io.IOException;

import static dev.al.FileManagerApplication.constants.Roles.ADMIN;
import static dev.al.FileManagerApplication.constants.Roles.USER;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;
    private final MessageSource messageSource;

    public FolderController(FolderService folderService, MessageSource messageSource) {
        this.folderService = folderService;
        this.messageSource = messageSource;
    }

    // Create a new folder under parent folder (or root if parentId is null)
    @PreAuthorize("hasAnyRole('" + ADMIN + "', '" + USER + "')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<FolderEntity>> createFolder(@RequestParam String name, @RequestParam(required = false) Long parentId) {
        FolderEntity folder = new FolderEntity();
        folder.setName(name);
        if (parentId != null) {
            folderService.getFolderById(parentId).ifPresent(folder::setParentFolder);
        }
        FolderEntity savedFolder = folderService.saveFolder(folder);
        return ResponseEntity.ok(ApiResponse.success(savedFolder, "action.success")); // "Action committed successfully"
    }

    // Get folder by ID
    private String extractUsername(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Optional<FolderEntity>>> getFolder(
            @PathVariable Long id,
            Authentication auth,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        Optional<FolderEntity> folder = folderService.getFolderById(id);
        String message = messageSource.getMessage("object.found", null, locale);
        return ResponseEntity.ok(ApiResponse.success(folder, message));
    }

    // Rename folder
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/rename/{id}")
    public ResponseEntity<ApiResponse<Void>> renameFolder(
            @PathVariable Long id,
            @RequestParam String newName,
            Authentication auth,
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {

        folderService.renameFolder(id, newName); // Pass name directly instead of FolderEntity
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
    // Move folder to new parent folder
    @PreAuthorize("hasAnyRole('" + ADMIN + "', '" + USER + "')")
    @PutMapping("/move/{id}")
    public ResponseEntity<ApiResponse<FolderEntity>> moveFolder(@PathVariable Long id, @RequestParam Long newParentFolderId) {
        FolderEntity movedFolder = folderService.moveFolder(id, newParentFolderId);
        return ResponseEntity.ok(ApiResponse.success(movedFolder, "action.success")); // "Action committed successfully"
    }

    // Delete folder by ID
    @PreAuthorize("hasRole('" + ADMIN + "')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "action.success")); // "Action committed successfully"
    }

    // List child folders by parent ID (null or 0 for root)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<FolderEntity>>> getFoldersByParent(@PathVariable Long parentId) {
        List<FolderEntity> folders = folderService.getFoldersByParentId(parentId);
        return ResponseEntity.ok(ApiResponse.success(folders, "action.success")); // "Action committed successfully"
    }

    // Search folders by name
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchFolders(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<FolderEntity> folderPage = folderService.searchFolders(filters, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("folders", folderPage.getContent().stream().map(folder -> Map.of(
                "id", folder.getId(),
                "name", folder.getName(),
                "createdBy", folder.getCreatedBy()
        )));
        data.put("currentPage", folderPage.getNumber());
        data.put("totalItems", folderPage.getTotalElements());
        data.put("totalPages", folderPage.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(data, "action.success")); // "Action committed successfully"
    }

    // Batch delete folders
    @PreAuthorize("hasRole('" + ADMIN + "')")
    @PostMapping("/folders/deleteBatch")
    public ResponseEntity<ApiResponse<Void>> deleteFoldersBatch(@RequestBody List<Long> folderIds) {
        folderService.deleteFoldersBatch(folderIds);
        return ResponseEntity.ok(ApiResponse.success(null, "action.success")); // "Action committed successfully"
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{folderId}/zip")
    public ResponseEntity<ApiResponse<Void>> zipFolder(
            @PathVariable Long folderId,
            @RequestParam("destinationZipPath") String destinationZipPath) {

        try {
            folderService.zipFolder(folderId, destinationZipPath);
            return ResponseEntity.ok(ApiResponse.success(null, "file.upload"));
            // "Upload file '{0}' to folder ID '{1}'" but we don't have the variables here, so better use "action.success" or a new message for "Folder zipped successfully"
            // I'll suggest to add a new key "folder.zipped=Folder zipped successfully" in messages and use that instead
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/unzip")
    public ResponseEntity<ApiResponse<Void>> unzipFolder(
            @RequestParam("zipFilePath") String zipFilePath,
            @RequestParam("destinationFolderPath") String destinationFolderPath) {

        try {
            folderService.unzipFolder(zipFilePath, destinationFolderPath);
            return ResponseEntity.ok(ApiResponse.success(null, "action.success")); // "Action committed successfully"
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{folderId}/copy")
    public ResponseEntity<ApiResponse<Void>> copyFolder(@PathVariable Long folderId,
                                                        @RequestParam Long destinationFolderId) {
        try {
            folderService.copyFolder(folderId, destinationFolderId);
            return ResponseEntity.ok(ApiResponse.success(null, "action.success")); // "Action committed successfully"
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
