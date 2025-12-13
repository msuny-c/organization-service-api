package ru.itmo.organization.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.model.ImportObjectType;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.model.ImportStatus;
import ru.itmo.organization.repository.ImportOperationRepository;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportOperationRepository importOperationRepository;
    private final ImportExecutorService importExecutorService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Transactional(noRollbackFor = Exception.class)
    public ImportOperationDto importObjects(
            MultipartFile file,
            ImportObjectType objectType,
            Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
            ImportOperation operation = startOperation(file, userContext.username(), objectType);
        try {
            List<?> records = parseFile(file, objectType);
            List<?> created = importExecutorService.executeImport(records, objectType);
            operation.markSuccess(created.size());
            importOperationRepository.save(operation);
            broadcastByType(objectType, !created.isEmpty());
            webSocketService.broadcastImportsUpdate();
            return ImportOperationDto.fromEntity(operation, created.stream()
                    .filter(OrganizationDto.class::isInstance)
                    .map(OrganizationDto.class::cast)
                    .toList());
        } catch (Exception ex) {
            operation.markFailed(extractMessage(ex));
            importOperationRepository.save(operation);
            webSocketService.broadcastImportsUpdate();
            if (ex instanceof IllegalArgumentException) {
                throw ex;
            }
            throw new IllegalArgumentException("Импорт не выполнен: " + extractMessage(ex), ex);
        }
    }

    public List<ImportOperationDto> listOperations(Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
        List<ImportOperation> operations = userContext.admin()
                ? importOperationRepository.findAll()
                : importOperationRepository.findAllForUser(userContext.username());
        return operations.stream()
                .map(ImportOperationDto::fromEntity)
                .collect(Collectors.toList());
    }

    public ImportOperationDto getOperation(Long id, Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
        ImportOperation operation = importOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Операция импорта не найдена"));

        if (!userContext.admin() && !operation.getUsername().equals(userContext.username())) {
            throw new ResourceNotFoundException("Операция импорта не найдена");
        }

        return ImportOperationDto.fromEntity(operation);
    }

    private ImportOperation startOperation(MultipartFile file, String username, ImportObjectType type) {
        ImportOperation operation = new ImportOperation();
        operation.setUsername(username == null || username.isBlank() ? "anonymous" : username.trim());
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(ImportStatus.IN_PROGRESS);
        operation.setObjectType(type == null ? ImportObjectType.ORGANIZATION : type);
        return importOperationRepository.save(operation);
    }

    private List<?> parseFile(MultipartFile file, ImportObjectType type) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл для импорта пуст");
        }
        try {
            ImportObjectType targetType = type == null ? ImportObjectType.ORGANIZATION : type;
            return switch (targetType) {
                case ORGANIZATION -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<ru.itmo.organization.dto.ImportOrganizationDto>>() {});
                case COORDINATES -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<ru.itmo.organization.dto.ImportCoordinatesDto>>() {});
                case LOCATION -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<ru.itmo.organization.dto.ImportLocationDto>>() {});
                case ADDRESS -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<ru.itmo.organization.dto.ImportAddressDto>>() {});
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать файл импорта: " + e.getMessage(), e);
        }
    }

    private String extractMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        Throwable cause = ex.getCause();
        return cause != null && cause.getMessage() != null ? cause.getMessage() : "Неизвестная ошибка";
    }

    private UserContext toUserContext(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return new UserContext("anonymous", false);
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        return new UserContext(authentication.getName(), admin);
    }

    private void broadcastByType(ImportObjectType type, boolean hasCreated) {
        if (!hasCreated) {
            return;
        }
        ImportObjectType resolvedType = type == null ? ImportObjectType.ORGANIZATION : type;
        switch (resolvedType) {
            case ORGANIZATION -> webSocketService.broadcastOrganizationsUpdate();
            case COORDINATES -> webSocketService.broadcastCoordinatesUpdate();
            case LOCATION -> webSocketService.broadcastLocationsUpdate();
            case ADDRESS -> webSocketService.broadcastAddressesUpdate();
        }
    }
}
