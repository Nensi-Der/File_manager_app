package dev.al.FileManagerApplication.controller;
import dev.al.FileManagerApplication.dto.AccessRequestDto;
import dev.al.FileManagerApplication.dto.AccessDecisionDto;
import dev.al.FileManagerApplication.service.AccessRequestService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/access")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;
    private final MessageSource messageSource;

    public AccessRequestController(AccessRequestService accessRequestService,
                                   MessageSource messageSource) {
        this.accessRequestService = accessRequestService;
        this.messageSource = messageSource;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestAccess(@RequestParam String requesterUsername,
                                           @RequestBody AccessRequestDto dto,
                                           Locale locale) {
        try {
            accessRequestService.requestAccess(requesterUsername, dto);
            String msg = messageSource.getMessage("action.success", null, locale);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @PostMapping("/decision")
    public ResponseEntity<?> handleAccessDecision(@RequestParam String ownerUsername,
                                                  @RequestBody AccessDecisionDto dto,
                                                  Locale locale) {
        try {
            accessRequestService.handleRequest(ownerUsername, dto);
            String key = dto.isApproved() ? "access.approved" : "access.denied";
            String msg = messageSource.getMessage(key, null, locale);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("action.failure", null, locale);
            return ResponseEntity.badRequest().body(msg);
        }
    }
}
