package dev.al.FileManagerApplication.controller;

import dev.al.FileManagerApplication.dto.ApiResponse;
import dev.al.FileManagerApplication.dto.ShareRequestDto;
import dev.al.FileManagerApplication.dto.SharedItemDto;
import dev.al.FileManagerApplication.service.SharingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/share")
public class SharingController {

    private final SharingService sharingService;

    public SharingController(SharingService sharingService) {
        this.sharingService = sharingService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> shareItem(@RequestBody ShareRequestDto dto) {
        sharingService.shareItem(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "sharing.item.shared.success"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SharedItemDto>>> getSharedItems(Authentication auth) {
        List<SharedItemDto> items = sharingService.getSharedItemsForUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(items, "sharing.items.fetched.success"));
    }
}