package ru.itmo.organization.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.service.ImportService;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class RestImportController {

    private final ImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportOperationDto> importOrganizations(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "objectType", defaultValue = "ORGANIZATION") ru.itmo.organization.model.ImportObjectType objectType,
            Authentication authentication) {

        ImportOperationDto dto = importService.importOrganizations(file, objectType, authentication);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ImportOperationDto>> listOperations(Authentication authentication) {
        return ResponseEntity.ok(importService.listOperations(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportOperationDto> getOperation(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(importService.getOperation(id, authentication));
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> downloadTemplate(
            @RequestParam(name = "objectType", defaultValue = "ORGANIZATION") ru.itmo.organization.model.ImportObjectType objectType) {
        String template = buildTemplate(objectType);
        ByteArrayResource resource = new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }

    private String buildTemplate(ru.itmo.organization.model.ImportObjectType type) {
        return switch (type == null ? ru.itmo.organization.model.ImportObjectType.ORGANIZATION : type) {
            case ORGANIZATION -> """
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
            case COORDINATES -> """
                    [
                      { "x": 10, "y": 20 },
                      { "x": 30, "y": 40 }
                    ]
                    """;
            case LOCATION -> """
                    [
                      { "x": 1, "y": 2, "z": 3.3, "name": "Location A" },
                      { "x": 5, "y": 6, "z": 7.7, "name": "Location B" }
                    ]
                    """;
            case ADDRESS -> """
                    [
                      {
                        "zipCode": "1970001",
                        "town": { "x": 1, "y": 1, "z": 1.1, "name": "Town A" }
                      },
                      {
                        "zipCode": "1970002",
                        "townId": 1
                      }
                    ]
                    """;
        };
    }
}
