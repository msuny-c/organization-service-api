package ru.itmo.organization.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.model.Address;
import ru.itmo.organization.model.Coordinates;
import ru.itmo.organization.model.Location;
import ru.itmo.organization.model.Organization;

@Component
@RequiredArgsConstructor
public class OrganizationMapper {

    private final ReferenceMapper referenceMapper;
    
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
        return referenceMapper.toDto(coordinates);
    }
    
    public Coordinates toEntity(CoordinatesDto dto) {
        return referenceMapper.toEntity(dto);
    }
    
    public AddressDto toDto(Address address) {
        return referenceMapper.toDto(address);
    }
    
    public Address toEntity(AddressDto dto) {
        return referenceMapper.toEntity(dto);
    }
    
    public LocationDto toDto(Location location) {
        return referenceMapper.toDto(location);
    }
    
    public Location toEntity(LocationDto dto) {
        return referenceMapper.toEntity(dto);
    }
}
