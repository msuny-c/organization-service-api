package ru.itmo.organization.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
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
    public ResponseEntity<ImportOperationDto> importObjects(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "objectType", defaultValue = "ORGANIZATION") ru.itmo.organization.model.ImportObjectType objectType,
            Authentication authentication) {

        ImportOperationDto dto = importService.importObjects(file, objectType, authentication);
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
        String template = readTemplate(objectType);
        ByteArrayResource resource = new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }

    private String readTemplate(ru.itmo.organization.model.ImportObjectType type) {
        ru.itmo.organization.model.ImportObjectType resolvedType =
                type == null ? ru.itmo.organization.model.ImportObjectType.ORGANIZATION : type;
        String filename = switch (resolvedType) {
            case ORGANIZATION -> "import-templates/organization.json";
            case COORDINATES -> "import-templates/coordinates.json";
            case LOCATION -> "import-templates/location.json";
            case ADDRESS -> "import-templates/address.json";
        };
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Не удалось загрузить шаблон импорта");
        }
    }
}
