package ru.itmo.organization.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.model.ImportObjectType;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.model.ImportStatus;
import ru.itmo.organization.repository.ImportOperationRepository;
import ru.itmo.organization.service.storage.StorageTransaction;

@Service
@RequiredArgsConstructor
public class ImportOperationLogService {

    private final ImportOperationRepository importOperationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation startOperation(String username, ImportObjectType type, StorageTransaction storageTx) {
        ImportOperation operation = new ImportOperation();
        operation.setUsername(normalizeUsername(username));
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(ImportStatus.IN_PROGRESS);
        operation.setObjectType(type == null ? ImportObjectType.ORGANIZATION : type);
        operation.setStorageFileName(storageTx.originalFileName());
        operation.setStorageContentType(storageTx.contentType());
        operation.setStorageSize(storageTx.size());
        return importOperationRepository.save(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation createFailedOperation(String username, ImportObjectType type, MultipartFile file) {
        ImportOperation operation = new ImportOperation();
        operation.setUsername(normalizeUsername(username));
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(ImportStatus.FAILED);
        operation.setObjectType(type == null ? ImportObjectType.ORGANIZATION : type);
        if (file != null) {
            operation.setStorageFileName(file.getOriginalFilename());
            operation.setStorageContentType(file.getContentType());
            operation.setStorageSize(file.getSize());
        }
        return importOperationRepository.save(operation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long operationId, String message) {
        if (operationId == null) {
            return;
        }
        importOperationRepository.findById(operationId).ifPresent(operation -> {
            operation.markFailed(message);
            importOperationRepository.save(operation);
        });
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "anonymous";
        }
        return username.trim();
    }
}
