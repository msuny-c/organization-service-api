package ru.itmo.organization.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.organization.model.ImportOperation;
import ru.itmo.organization.model.ImportStatus;

@Getter
@Setter
@NoArgsConstructor
public class ImportOperationDto {

    private Long id;
    private ImportStatus status;
    private ru.itmo.organization.model.ImportObjectType objectType;
    private String username;
    private java.time.LocalDateTime startedAt;
    private Integer addedCount;
    private List<OrganizationDto> createdOrganizations;
    private String storageFileName;
    private String storageContentType;
    private Long storageSize;

    public static ImportOperationDto fromEntity(ImportOperation operation) {
        ImportOperationDto dto = new ImportOperationDto();
        dto.setId(operation.getId());
        dto.setStatus(operation.getStatus());
        dto.setObjectType(operation.getObjectType());
        dto.setUsername(operation.getUsername());
        dto.setStartedAt(operation.getStartedAt());
        dto.setAddedCount(operation.getAddedCount());
        dto.setStorageFileName(operation.getStorageFileName());
        dto.setStorageContentType(operation.getStorageContentType());
        dto.setStorageSize(operation.getStorageSize());
        return dto;
    }

    public static ImportOperationDto fromEntity(ImportOperation operation, List<OrganizationDto> created) {
        ImportOperationDto dto = fromEntity(operation);
        dto.setCreatedOrganizations(created);
        return dto;
    }
}
