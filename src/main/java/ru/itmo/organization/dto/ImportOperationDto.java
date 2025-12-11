package ru.itmo.organization.dto;

import java.time.LocalDateTime;
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
    private String username;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer addedCount;
    private String filename;
    private String errorMessage;

    public static ImportOperationDto fromEntity(ImportOperation operation) {
        ImportOperationDto dto = new ImportOperationDto();
        dto.setId(operation.getId());
        dto.setStatus(operation.getStatus());
        dto.setUsername(operation.getUsername());
        dto.setStartedAt(operation.getStartedAt());
        dto.setFinishedAt(operation.getFinishedAt());
        dto.setAddedCount(operation.getAddedCount());
        dto.setFilename(operation.getFilename());
        dto.setErrorMessage(operation.getErrorMessage());
        return dto;
    }
}
