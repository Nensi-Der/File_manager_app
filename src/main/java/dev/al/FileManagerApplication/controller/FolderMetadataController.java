package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderMetadataController {

    private final FolderService folderService;

    public FolderMetadataController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{folderId}/metadata")
    public ResponseEntity<ApiResponse<Void>> addMetadata(@PathVariable Long folderId,
                                                         @RequestBody MetadataDto metadataDto) {
        folderService.addMetadata(folderId, metadataDto);
        return ResponseEntity.ok(ApiResponse.success(null, "metadata.add.success"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{folderId}/metadata/{key}")
    public ResponseEntity<ApiResponse<Void>> updateMetadata(@PathVariable Long folderId,
                                                            @PathVariable String key,
                                                            @RequestBody MetadataDto metadataDto) {
        folderService.updateMetadata(folderId, key, metadataDto);
        return ResponseEntity.ok(ApiResponse.success(null, "metadata.update.success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{folderId}/metadata/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteMetadata(@PathVariable Long folderId,
                                                            @PathVariable String key) {
        folderService.deleteMetadata(folderId, key);
        return ResponseEntity.ok(ApiResponse.success(null, "metadata.delete.success"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{folderId}/metadata")
    public ResponseEntity<ApiResponse<List<MetadataDto>>> getMetadata(@PathVariable Long folderId) {
        List<MetadataDto> metadata = folderService.getMetadata(folderId);
        return ResponseEntity.ok(ApiResponse.success(metadata, "metadata.fetch.success"));
    }
}