package ru.itmo.organization.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.model.ImportObjectType;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.repository.ImportOperationRepository;
import ru.itmo.organization.service.storage.StorageService;
import ru.itmo.organization.service.storage.StorageTransaction;

@Service
@RequiredArgsConstructor
public class ImportTransactionService {

    private final ImportOperationRepository importOperationRepository;
    private final ImportExecutorService importExecutorService;
    private final StorageService storageService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<?> executeImport(
            Long operationId,
            List<?> records,
            ImportObjectType objectType,
            StorageTransaction storageTx) {
        if (operationId == null) {
            throw new ResourceNotFoundException("Операция импорта не найдена");
        }

        ImportOperation managed = importOperationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Операция импорта не найдена"));

        List<?> imported = importExecutorService.executeImport(records, objectType);
        managed.setStorageBucket(storageTx.bucket());
        managed.setStorageObject(storageTx.finalObjectName());
        managed.setStorageFileName(storageTx.originalFileName());
        managed.setStorageContentType(storageTx.contentType());
        managed.setStorageSize(storageTx.size());
        managed.markSuccess(imported.size());
        importOperationRepository.save(managed);

        storageService.commit(storageTx);
        return imported;
    }
}
