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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.ImportOperationDto;
import ru.itmo.organization.dto.LocationDto;
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
    private final OrganizationService organizationService;
    private final CoordinatesService coordinatesService;
    private final LocationService locationService;
    private final AddressService addressService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Transactional(noRollbackFor = Exception.class)
    public ImportOperationDto importOrganizations(
            MultipartFile file,
            ImportObjectType objectType,
            Authentication authentication) {
        UserContext userContext = toUserContext(authentication);
        ImportOperation operation = startOperation(file, userContext.username(), objectType);
        try {
            List<?> records = parseFile(file, objectType);
            List<?> created = executeTransactionalImport(records, objectType);
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
                case ORGANIZATION -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<OrganizationDto>>() {});
                case COORDINATES -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<CoordinatesDto>>() {});
                case LOCATION -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<LocationDto>>() {});
                case ADDRESS -> objectMapper.readValue(file.getInputStream(), new TypeReference<List<AddressDto>>() {});
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать файл импорта: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    private List<?> executeTransactionalImport(List<?> records, ImportObjectType type) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта");
        }

        List<Object> created = new java.util.ArrayList<>();
        int index = 1;
        for (Object dto : records) {
            try {
                created.add(handleSingleImport(dto, type));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Ошибка в записи #" + index + ": " + conciseMessage(ex), ex);
            }
            index++;
        }
        return created;
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

    private Object handleSingleImport(Object dto, ImportObjectType type) {
        ImportObjectType resolvedType = type == null ? ImportObjectType.ORGANIZATION : type;
        return switch (resolvedType) {
            case ORGANIZATION -> organizationService.create((OrganizationDto) dto);
            case COORDINATES -> coordinatesService.create((CoordinatesDto) dto);
            case LOCATION -> locationService.create((LocationDto) dto);
            case ADDRESS -> importAddress((AddressDto) dto);
        };
    }

    private AddressDto importAddress(AddressDto dto) {
        AddressDto payload = dto;
        if (dto.getTownId() == null && dto.getTown() == null) {
            throw new IllegalArgumentException("Не указан город для адреса");
        }
        if (dto.getTownId() == null && dto.getTown() != null) {
            LocationDto locationDto = dto.getTown();
            LocationDto savedTown = locationService.create(locationDto);
            payload = new AddressDto(
                    dto.getId(),
                    dto.getZipCode(),
                    savedTown.getId(),
                    null,
                    dto.getIsUpdated()
            );
        }
        AddressDto saved = addressService.create(payload);
        return saved;
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
