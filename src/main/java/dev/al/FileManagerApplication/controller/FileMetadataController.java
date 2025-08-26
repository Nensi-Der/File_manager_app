package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.dto.MetadataDto;
import dev.al.FileManagerApplication.service.FileService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/files")
public class FileMetadataController {

    private final FileService fileService;
    private final MessageSource messageSource;

    public FileMetadataController(FileService fileService, MessageSource messageSource) {
        this.fileService = fileService;
        this.messageSource = messageSource;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<Void>> addMetadata(@PathVariable Long fileId,
                                                         @RequestBody MetadataDto metadataDto,
                                                         @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        fileService.addMetadata(fileId, metadataDto);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{fileId}/metadata/{key}")
    public ResponseEntity<ApiResponse<Void>> updateMetadata(@PathVariable Long fileId,
                                                            @PathVariable String key,
                                                            @RequestBody MetadataDto metadataDto,
                                                            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        fileService.updateMetadata(fileId, key, metadataDto);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{fileId}/metadata/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteMetadata(@PathVariable Long fileId,
                                                            @PathVariable String key,
                                                            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        fileService.deleteMetadata(fileId, key);
        String message = messageSource.getMessage("action.success", null, locale);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<ApiResponse<List<MetadataDto>>> getMetadata(@PathVariable Long fileId,
                                                                      @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        List<MetadataDto> metadata = fileService.getMetadata(fileId);
        String message = messageSource.getMessage("object.found", null, locale);
        return ResponseEntity.ok(ApiResponse.success(metadata, message));
    }
}
