package ru.itmo.organization.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.exception.StorageUnavailableException;
import ru.itmo.organization.model.ImportObjectType;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.repository.ImportOperationRepository;
import ru.itmo.organization.service.storage.StorageService;
import ru.itmo.organization.service.storage.StorageTransaction;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ImportOperationRepository importOperationRepository;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ImportTransactionService importTransactionService;
    private final ImportOperationLogService importOperationLogService;

    public ImportService(
            ImportOperationRepository importOperationRepository,
            WebSocketService webSocketService,
            ObjectMapper objectMapper,
            StorageService storageService,
            ImportTransactionService importTransactionService,
            ImportOperationLogService importOperationLogService) {
        this.importOperationRepository = importOperationRepository;
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.importTransactionService = importTransactionService;
        this.importOperationLogService = importOperationLogService;
    }

    public ImportOperationDto importObjects(
            MultipartFile file,
            ImportObjectType objectType,
            Authentication authentication) {

        UserContext userContext = toUserContext(authentication);
        StorageTransaction storageTx = null;
        ImportOperation operation = null;
        try {
            storageTx = storageService.stageImportFile(file);
            operation = importOperationLogService.startOperation(userContext.username(), objectType, storageTx);

            List<?> records = parseFile(file, objectType);
            List<?> created = importTransactionService.executeImport(
                    operation == null ? null : operation.getId(),
                    records,
                    objectType,
                    storageTx);

            webSocketService.broadcastImportsUpdate();
            if (created == null) {
                throw new IllegalStateException("Импорт не выполнен");
            }
            broadcastByType(objectType, !created.isEmpty());
            return ImportOperationDto.fromEntity(
                    importOperationRepository.findById(operation.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Операция импорта не найдена")),
                    created.stream()
                            .filter(OrganizationDto.class::isInstance)
                            .map(OrganizationDto.class::cast)
                            .toList());
        } catch (Exception ex) {
            storageService.rollback(storageTx);
            safeMarkFailed(operation, extractMessage(ex));
            if (operation == null && ex instanceof StorageUnavailableException) {
                operation = importOperationLogService.createFailedOperation(userContext.username(), objectType, file);
            }
            webSocketService.broadcastImportsUpdate();
            if (ex instanceof StorageUnavailableException) {
                log.error("Импорт не выполнен: хранилище недоступно (user={}, type={})",
                        userContext.username(), objectType, ex);
                throw (StorageUnavailableException) ex;
            }
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

    public ImportOperation getOperationEntity(Long id, Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
        ImportOperation operation = importOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Операция импорта не найдена"));

        if (!userContext.admin() && !operation.getUsername().equals(userContext.username())) {
            throw new ResourceNotFoundException("Операция импорта не найдена");
        }

        return operation;
    }

    private void safeMarkFailed(ImportOperation operation, String message) {
        if (operation == null || operation.getId() == null) {
            return;
        }
        try {
            importOperationLogService.markFailed(operation.getId(), message);
        } catch (Exception ex) {
            log.warn("Не удалось зафиксировать статус ошибки импорта: {}", ex.getMessage());
        }
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
