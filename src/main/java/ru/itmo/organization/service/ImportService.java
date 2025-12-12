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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.model.ImportStatus;
import ru.itmo.organization.repository.ImportOperationRepository;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportOperationRepository importOperationRepository;
    private final OrganizationService organizationService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public ImportOperationDto importOrganizations(MultipartFile file, Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
        ImportOperation operation = startOperation(file, userContext.username());
        try {
            List<OrganizationDto> records = parseFile(file);
            List<OrganizationDto> created = executeTransactionalImport(records);
            operation.markSuccess(created.size());
            importOperationRepository.save(operation);
            if (!created.isEmpty()) {
                webSocketService.broadcastOrganizationsUpdate();
            }
            webSocketService.broadcastImportsUpdate();
            return ImportOperationDto.fromEntity(operation, created);
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

    private ImportOperation startOperation(MultipartFile file, String username) {
        ImportOperation operation = new ImportOperation();
        operation.setUsername(username == null || username.isBlank() ? "anonymous" : username.trim());
        operation.setFilename(file != null ? file.getOriginalFilename() : null);
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(ImportStatus.IN_PROGRESS);
        return importOperationRepository.save(operation);
    }

    private List<OrganizationDto> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл для импорта пуст");
        }
        try {
            return objectMapper.readValue(file.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать файл импорта: " + e.getMessage(), e);
        }
    }

    private List<OrganizationDto> executeTransactionalImport(List<OrganizationDto> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта");
        }
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setName("importOrganizations");
        definition.setIsolationLevel(DefaultTransactionDefinition.ISOLATION_SERIALIZABLE);
        TransactionStatus status = transactionManager.getTransaction(definition);

        try {
            List<OrganizationDto> created = new java.util.ArrayList<>();
            int index = 1;
            for (OrganizationDto dto : records) {
                try {
                    OrganizationDto saved = organizationService.create(dto);
                    created.add(saved);
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Ошибка в записи #" + index + ": " + conciseMessage(ex), ex);
                }
                index++;
            }
            transactionManager.commit(status);
            return created;
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
            throw ex;
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

    private String conciseMessage(Exception ex) {
        String msg = extractMessage(ex);
        msg = msg.replace("create.dto.", "");

        String cleaned = msg
                .replace("fullName:", "")
                .replace("coordinates:", "")
                .replace("postalAddress.zipCode:", "")
                .replace("postalAddress:", "")
                .replace(":", "");

        String[] parts = cleaned.split(",");
        return java.util.Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + "; " + b)
                .orElse(cleaned);
    }

    private UserContext toUserContext(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return new UserContext("anonymous", false);
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        return new UserContext(authentication.getName(), admin);
    }
}
