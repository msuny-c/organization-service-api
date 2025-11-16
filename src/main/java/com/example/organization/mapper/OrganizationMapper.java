package com.example.organization.mapper;

import com.example.organization.dto.*;
import com.example.organization.model.*;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {
    
    public OrganizationDto toDto(Organization organization) {
        if (organization == null) {
            return null;
        }
        
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setCreationDate(organization.getCreationDate());
        dto.setAnnualTurnover(organization.getAnnualTurnover());
        dto.setEmployeesCount(organization.getEmployeesCount());
        dto.setRating(organization.getRating());
        dto.setFullName(organization.getFullName());
        dto.setType(organization.getType());
        
        if (organization.getCoordinates() != null) {
            dto.setCoordinatesId(organization.getCoordinates().getId());
            dto.setCoordinates(toDto(organization.getCoordinates()));
        }
        
        if (organization.getOfficialAddress() != null) {
            dto.setOfficialAddressId(organization.getOfficialAddress().getId());
            dto.setOfficialAddress(toDto(organization.getOfficialAddress()));
        }
        
        if (organization.getPostalAddress() != null) {
            dto.setPostalAddress(toDto(organization.getPostalAddress()));
        }
        
        return dto;
    }
    
    public Organization toEntity(OrganizationDto dto) {
        if (dto == null) {
            return null;
        }
        
        Organization organization = new Organization();
        organization.setId(dto.getId());
        organization.setName(dto.getName());
        organization.setCreationDate(dto.getCreationDate());
        organization.setAnnualTurnover(dto.getAnnualTurnover());
        organization.setEmployeesCount(dto.getEmployeesCount());
        organization.setRating(dto.getRating());
        organization.setFullName(dto.getFullName());
        organization.setType(dto.getType());
        
        return organization;
    }
    
    public CoordinatesDto toDto(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        
        return new CoordinatesDto(
            coordinates.getId(),
            coordinates.getX(),
            coordinates.getY()
        );
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
        return address;
    }
    
    public LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        
        return new LocationDto(
            location.getId(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getName()
        );
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
