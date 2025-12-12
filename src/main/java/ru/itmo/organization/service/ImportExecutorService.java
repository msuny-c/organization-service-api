package ru.itmo.organization.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.model.ImportObjectType;

@Service
@RequiredArgsConstructor
public class ImportExecutorService {

    private final OrganizationService organizationService;
    private final CoordinatesService coordinatesService;
    private final LocationService locationService;
    private final AddressService addressService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public List<?> executeImport(List<?> records, ImportObjectType type) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта");
        }

        List<Object> created = new ArrayList<>();
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
        if (dto == null) {
            throw new IllegalArgumentException("Адрес обязателен");
        }
        if (dto.getTownId() == null && dto.getTown() == null) {
            throw new IllegalArgumentException("Не указан город для адреса");
        }
        AddressDto payload = dto;
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
        return addressService.create(payload);
    }

    private String conciseMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Неизвестная ошибка";
        }

        String cleaned = message
                .replace("create.dto.", "")
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
}
