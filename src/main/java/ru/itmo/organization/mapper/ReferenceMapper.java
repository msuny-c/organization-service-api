package ru.itmo.organization.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.model.Address;
import ru.itmo.organization.model.Coordinates;
import ru.itmo.organization.model.Location;

@Component
public class ReferenceMapper {

    public CoordinatesDto toDto(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        CoordinatesDto dto = new CoordinatesDto();
        dto.setId(coordinates.getId());
        dto.setX(coordinates.getX());
        dto.setY(coordinates.getY());
        return dto;
    }

    public Coordinates toEntity(CoordinatesDto dto) {
        if (dto == null) {
            return null;
        }
        Coordinates coordinates = new Coordinates();
        coordinates.setId(dto.getId());
        coordinates.setX(dto.getX());
        coordinates.setY(dto.getY());
        return coordinates;
    }

    public AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        AddressDto dto = new AddressDto();
        dto.setId(address.getId());
        dto.setZipCode(address.getZipCode());
        if (address.getTown() != null) {
            dto.setTownId(address.getTown().getId());
            dto.setTown(toDto(address.getTown()));
        }
        return dto;
    }

    public Address toEntity(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        Address address = new Address();
        address.setId(dto.getId());
        address.setZipCode(dto.getZipCode());
        if (dto.getTownId() != null) {
            Location town = new Location();
            town.setId(dto.getTownId());
            address.setTown(town);
        }
        if (dto.getTown() != null && dto.getTownId() == null) {
            address.setTown(toEntity(dto.getTown()));
        }
        return address;
    }

    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setX(location.getX());
        dto.setY(location.getY());
        dto.setZ(location.getZ());
        dto.setName(location.getName());
        return dto;
    }

    public Location toEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        Location location = new Location();
        location.setId(dto.getId());
        location.setX(dto.getX());
        location.setY(dto.getY());
        location.setZ(dto.getZ());
        location.setName(dto.getName());
        return location;
    }
}
