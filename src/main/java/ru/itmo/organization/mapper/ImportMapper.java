package ru.itmo.organization.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.ImportAddressDto;
import ru.itmo.organization.dto.ImportCoordinatesDto;
import ru.itmo.organization.dto.ImportLocationDto;
import ru.itmo.organization.dto.ImportOrganizationDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.dto.OrganizationDto;

@Component
public class ImportMapper {

    public OrganizationDto toOrganizationDto(ImportOrganizationDto source) {
        if (source == null) {
            throw new IllegalArgumentException("Организация обязательна");
        }
        OrganizationDto dto = new OrganizationDto();
        dto.setName(source.getName());
        dto.setAnnualTurnover(source.getAnnualTurnover());
        dto.setEmployeesCount(source.getEmployeesCount());
        dto.setRating(source.getRating());
        dto.setFullName(source.getFullName());
        dto.setType(source.getType());
        dto.setCoordinates(toCoordinatesDto(source.getCoordinates()));
        dto.setPostalAddress(toAddressDto(source.getPostalAddress()));
        dto.setOfficialAddress(toAddressDto(source.getOfficialAddress()));
        return dto;
    }

    public CoordinatesDto toCoordinatesDto(ImportCoordinatesDto source) {
        if (source == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        CoordinatesDto dto = new CoordinatesDto();
        dto.setX(source.getX());
        dto.setY(source.getY());
        return dto;
    }

    public AddressDto toAddressDto(ImportAddressDto source) {
        if (source == null) {
            return null;
        }
        LocationDto town = toLocationDto(source.getTown());
        return new AddressDto(
                null,
                source.getZipCode(),
                null,
                town,
                null
        );
    }

    public LocationDto toLocationDto(ImportLocationDto source) {
        if (source == null) {
            throw new IllegalArgumentException("Локация обязательна");
        }
        LocationDto dto = new LocationDto();
        dto.setX(source.getX());
        dto.setY(source.getY());
        dto.setZ(source.getZ());
        dto.setName(source.getName());
        return dto;
    }
}
