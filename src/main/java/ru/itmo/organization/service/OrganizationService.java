package ru.itmo.organization.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.model.*;
import ru.itmo.organization.repository.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final AddressRepository addressRepository;
    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;
    
    @Transactional(readOnly = true)
    public Page<OrganizationDto> findAll(Pageable pageable) {
        return organizationRepository.findAllWithDetails(pageable)
                .map(mapper::toDto);
    }
    
    @Transactional(readOnly = true)
    public Page<OrganizationDto> findBySearchTerm(String searchTerm, String searchField, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(pageable);
        }
        return organizationRepository.search(searchTerm, searchField, pageable)
                .map(mapper::toDto);
    }
    
    @Transactional(readOnly = true)
    public OrganizationDto findById(Long id) {
        Organization organization = organizationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        return mapper.toDto(organization);
    }
    
    public OrganizationDto create(OrganizationDto dto) {
        Organization organization = mapper.toEntity(dto);
        organization.setCreationDate(LocalDate.now());
        
        organization.setCoordinates(getOrCreateCoordinates(dto));
        
        Address postalAddress = getOrCreateAddress(dto.getPostalAddressId(), dto.getPostalAddress());
        organization.setPostalAddress(postalAddress);
        
        if (Boolean.TRUE.equals(dto.getReusePostalAddressAsOfficial())) {
            organization.setOfficialAddress(postalAddress);
        } else if (dto.getOfficialAddressId() != null || dto.getOfficialAddress() != null) {
            organization.setOfficialAddress(getOrCreateAddress(dto.getOfficialAddressId(), dto.getOfficialAddress()));
        }
        
        Organization saved = organizationRepository.save(organization);
        return mapper.toDto(saved);
    }
    
    public OrganizationDto update(Long id, OrganizationDto dto) {
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        
        existing.setName(dto.getName());
        existing.setAnnualTurnover(dto.getAnnualTurnover());
        existing.setEmployeesCount(dto.getEmployeesCount());
        existing.setRating(dto.getRating());
        existing.setFullName(dto.getFullName());
        existing.setType(dto.getType());
        
        if (dto.getCoordinatesId() != null || dto.getCoordinates() != null) {
            existing.setCoordinates(getOrCreateCoordinates(dto));
        }
        
        Address postalAddress = null;
        if (dto.getPostalAddressId() != null || dto.getPostalAddress() != null) {
            postalAddress = getOrCreateAddress(dto.getPostalAddressId(), dto.getPostalAddress());
            existing.setPostalAddress(postalAddress);
        }
        
        if (Boolean.TRUE.equals(dto.getReusePostalAddressAsOfficial()) && postalAddress != null) {
            existing.setOfficialAddress(postalAddress);
        } else if (dto.getOfficialAddressId() != null || dto.getOfficialAddress() != null) {
            existing.setOfficialAddress(getOrCreateAddress(dto.getOfficialAddressId(), dto.getOfficialAddress()));
        }
        
        Organization updated = organizationRepository.save(existing);
        return mapper.toDto(updated);
    }
    
    public void delete(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        
        Coordinates coordinates = organization.getCoordinates();
        Address officialAddress = organization.getOfficialAddress();
        Address postalAddress = organization.getPostalAddress();
        
        organizationRepository.delete(organization);
        
        cleanupOrphanedObjects(coordinates, officialAddress, postalAddress);
    }
    
    @Transactional(readOnly = true)
    public OrganizationDto findOneWithMinimalCoordinates() {
        List<Organization> organizations = organizationRepository.findAllOrderedByCoordinatesWithDetails();
        if (organizations.isEmpty()) {
            throw new ResourceNotFoundException("Организации не найдены");
        }
        return mapper.toDto(organizations.get(0));
    }
    
    @Transactional(readOnly = true)
    public Map<Integer, Long> groupByRating() {
        List<Object[]> results = organizationRepository.countByRatingGrouped();
        return results.stream()
                .collect(Collectors.toMap(
                    result -> ((Number) result[0]).intValue(),
                    result -> ((Number) result[1]).longValue()
                ));
    }
    
    @Transactional(readOnly = true)
    public long countByType(OrganizationType type) {
        return organizationRepository.countByType(type);
    }
    
    public OrganizationDto dismissAllEmployees(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        
        organization.setEmployeesCount(0);
        Organization updated = organizationRepository.save(organization);
        return mapper.toDto(updated);
    }
    
    public OrganizationDto absorb(Long absorbingId, Long absorbedId) {
        if (absorbingId.equals(absorbedId)) {
            throw new IllegalArgumentException("Организация не может поглотить саму себя");
        }
        
        Organization absorbing = organizationRepository.findById(absorbingId)
                .orElseThrow(() -> new ResourceNotFoundException("Поглощающая организация с ID " + absorbingId + " не найдена"));
        
        Organization absorbed = organizationRepository.findById(absorbedId)
                .orElseThrow(() -> new ResourceNotFoundException("Поглощаемая организация с ID " + absorbedId + " не найдена"));
        
        absorbing.setEmployeesCount(absorbing.getEmployeesCount() + absorbed.getEmployeesCount());
        
        Coordinates coordinates = absorbed.getCoordinates();
        Address officialAddress = absorbed.getOfficialAddress();
        Address postalAddress = absorbed.getPostalAddress();
        
        organizationRepository.delete(absorbed);
        cleanupOrphanedObjects(coordinates, officialAddress, postalAddress);
        
        Organization updated = organizationRepository.save(absorbing);
        return mapper.toDto(updated);
    }
    
    private Coordinates getOrCreateCoordinates(OrganizationDto dto) {
        if (dto.getCoordinatesId() != null) {
            return coordinatesRepository.findById(dto.getCoordinatesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + dto.getCoordinatesId() + " не найдены"));
        } else if (dto.getCoordinates() != null) {
            Coordinates coordinates = mapper.toEntity(dto.getCoordinates());
            return coordinatesRepository.save(coordinates);
        } else {
            throw new IllegalArgumentException("Необходимо указать координаты");
        }
    }
    
    private Address getOrCreateAddress(Long addressId, ru.itmo.organization.dto.AddressDto addressDto) {
        if (addressId != null) {
            return addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + addressId + " не найден"));
        } else if (addressDto != null) {
            if (addressDto.getZipCode() != null && addressDto.getZipCode().length() < 7) {
                throw new IllegalArgumentException("Почтовый индекс должен содержать минимум 7 символов");
            }
            
            Address address = mapper.toEntity(addressDto);
            
            if (addressDto.getTownId() != null) {
                Location town = locationRepository.findById(addressDto.getTownId())
                        .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + addressDto.getTownId() + " не найдена"));
                address.setTown(town);
            } else if (addressDto.getTown() != null && isLocationValid(addressDto.getTown())) {
                Location town = mapper.toEntity(addressDto.getTown());
                town = locationRepository.save(town);
                address.setTown(town);
            } else {
                throw new IllegalArgumentException("Необходимо указать город для адреса");
            }
            
            return addressRepository.save(address);
        }
        throw new IllegalArgumentException("Необходимо указать адрес");
    }
    
    private boolean isLocationValid(ru.itmo.organization.dto.LocationDto locationDto) {
        return locationDto.getX() != null && 
               locationDto.getY() != null && 
               locationDto.getZ() != null && 
               locationDto.getName() != null && !locationDto.getName().trim().isEmpty();
    }
    
    private void cleanupOrphanedObjects(Coordinates coordinates, Address officialAddress, Address postalAddress) {
        if (coordinates != null) {
            List<Coordinates> orphanedCoordinates = coordinatesRepository.findOrphaned();
            if (orphanedCoordinates.stream().anyMatch(c -> c.getId().equals(coordinates.getId()))) {
                coordinatesRepository.delete(coordinates);
            }
        }
        
        if (officialAddress != null) {
            List<Address> orphanedAddresses = addressRepository.findOrphaned();
            if (orphanedAddresses.stream().anyMatch(a -> a.getId().equals(officialAddress.getId()))) {
                Location town = officialAddress.getTown();
                addressRepository.delete(officialAddress);
                
                if (town != null) {
                    List<Location> orphanedLocations = locationRepository.findOrphaned();
                    if (orphanedLocations.stream().anyMatch(l -> l.getId().equals(town.getId()))) {
                        locationRepository.delete(town);
                    }
                }
            }
        }
        
        if (postalAddress != null && !postalAddress.equals(officialAddress)) {
            List<Address> orphanedAddresses = addressRepository.findOrphaned();
            if (orphanedAddresses.stream().anyMatch(a -> a.getId().equals(postalAddress.getId()))) {
                Location town = postalAddress.getTown();
                addressRepository.delete(postalAddress);
                
                if (town != null) {
                    List<Location> orphanedLocations = locationRepository.findOrphaned();
                    if (orphanedLocations.stream().anyMatch(l -> l.getId().equals(town.getId()))) {
                        locationRepository.delete(town);
                    }
                }
            }
        }
    }
}
