package ru.itmo.organization.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.service.ImportService;
import ru.itmo.organization.service.UserContext;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class RestImportController {

    private final ImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportOperationDto> importOrganizations(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", required = false) String usernameParam,
            @RequestParam(value = "admin", defaultValue = "false") boolean admin,
            @RequestHeader(value = "X-User", required = false) String usernameHeader) {

        UserContext userContext = resolveUserContext(usernameParam != null ? usernameParam : usernameHeader, admin);
        ImportOperationDto dto = importService.importOrganizations(file, userContext);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ImportOperationDto>> listOperations(
            @RequestParam(value = "username", required = false) String usernameParam,
            @RequestParam(value = "admin", defaultValue = "false") boolean admin,
            @RequestHeader(value = "X-User", required = false) String usernameHeader) {

        UserContext userContext = resolveUserContext(usernameParam != null ? usernameParam : usernameHeader, admin);
        return ResponseEntity.ok(importService.listOperations(userContext));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportOperationDto> getOperation(
            @PathVariable Long id,
            @RequestParam(value = "username", required = false) String usernameParam,
            @RequestParam(value = "admin", defaultValue = "false") boolean admin,
            @RequestHeader(value = "X-User", required = false) String usernameHeader) {

        UserContext userContext = resolveUserContext(usernameParam != null ? usernameParam : usernameHeader, admin);
        return ResponseEntity.ok(importService.getOperation(id, userContext));
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadTemplate() {
        String template = """
                [
                  {
                    "name": "Demo Org",
                    "coordinates": { "x": 100, "y": 200 },
                    "employeesCount": 50,
                    "annualTurnover": 500000,
                    "rating": 4,
                    "fullName": "Demo Org LLC",
                    "type": "COMMERCIAL",
                    "postalAddress": {
                      "zipCode": "1900001",
                      "town": { "x": 1, "y": 2, "z": 3.5, "name": "Sample Town" }
                    },
                    "officialAddress": {
                      "zipCode": "1900002",
                      "town": { "x": 4, "y": 5, "z": 6.5, "name": "Another Town" }
                    }
                  }
                ]
                """;
        ByteArrayResource resource = new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }

    private UserContext resolveUserContext(String rawUsername, boolean admin) {
        String username = (rawUsername == null || rawUsername.isBlank()) ? "anonymous" : rawUsername.trim();
        return admin ? UserContext.admin(username) : UserContext.regular(username);
    }
}
