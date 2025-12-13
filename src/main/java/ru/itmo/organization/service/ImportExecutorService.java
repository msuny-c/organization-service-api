package ru.itmo.organization.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.mapper.ImportMapper;
import ru.itmo.organization.model.ImportObjectType;

@Service
@RequiredArgsConstructor
public class ImportExecutorService {

    private final OrganizationService organizationService;
    private final CoordinatesService coordinatesService;
    private final LocationService locationService;
    private final AddressService addressService;
    private final ImportMapper importMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
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
            case ORGANIZATION -> organizationService.create(importMapper.toOrganizationDto((ru.itmo.organization.dto.ImportOrganizationDto) dto));
            case COORDINATES -> coordinatesService.create(importMapper.toCoordinatesDto((ru.itmo.organization.dto.ImportCoordinatesDto) dto));
            case LOCATION -> locationService.create(importMapper.toLocationDto((ru.itmo.organization.dto.ImportLocationDto) dto));
            case ADDRESS -> importAddress((ru.itmo.organization.dto.ImportAddressDto) dto);
        };
    }

    private AddressDto importAddress(ru.itmo.organization.dto.ImportAddressDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Адрес обязателен");
        }
        LocationDto savedTown = locationService.create(importMapper.toLocationDto(dto.getTown()));
        AddressDto payload = new AddressDto(
                null,
                dto.getZipCode(),
                savedTown.getId(),
                null,
                null
        );
        return addressService.create(payload);
    }

    private String conciseMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Неизвестная ошибка";
        }
        String cleaned = message
                .replace("create.dto.", "")
                .replace("dto.", "");

        int colonIndex = cleaned.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < cleaned.length()) {
            String tail = cleaned.substring(colonIndex + 1).trim();
            if (!tail.isEmpty()) {
                return tail;
            }
        }
        return cleaned.trim();
    }
}
