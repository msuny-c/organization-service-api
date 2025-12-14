package ru.itmo.organization.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.model.*;
import ru.itmo.organization.repository.*;
import ru.itmo.organization.validation.UniqueOrganization;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final AddressRepository addressRepository;
    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;
    private final WebSocketService webSocketService;
    
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
    
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class, TransactionSystemException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 1000, random = true))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrganizationDto create(@Valid @UniqueOrganization OrganizationDto dto) {
        Organization organization = mapper.toEntity(dto);
        organization.setCreationDate(LocalDate.now());
        
        organization.setCoordinates(getOrCreateCoordinates(dto));
        
        Address postalAddress = getOrCreateAddress(dto.getPostalAddressId(), dto.getPostalAddress());
        organization.setPostalAddress(postalAddress);
        
        if (Boolean.TRUE.equals(dto.getReusePostalAddressAsOfficial())) {
            organization.setOfficialAddress(postalAddress);
        } else if (dto.getOfficialAddressId() != null || dto.getOfficialAddress() != null) {
            organization.setOfficialAddress(getOrCreateAddress(dto.getOfficialAddressId(), dto.getOfficialAddress()));
        } else {
            organization.setOfficialAddress(null);
        }
        
        Organization saved = organizationRepository.save(organization);
        webSocketService.broadcastOrganizationsUpdate();
        return mapper.toDto(saved);
    }
    
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class, TransactionSystemException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 1000, random = true))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrganizationDto update(Long id, @Valid @UniqueOrganization OrganizationDto dto) {
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
        } else {
            existing.setOfficialAddress(null);
        }
        
        Organization updated = organizationRepository.save(existing);
        webSocketService.broadcastOrganizationsUpdate();
        return mapper.toDto(updated);
    }
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void delete(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        
        Coordinates coordinates = organization.getCoordinates();
        Address officialAddress = organization.getOfficialAddress();
        Address postalAddress = organization.getPostalAddress();
        
        organizationRepository.delete(organization);
        
        cleanupOrphanedObjects(coordinates, officialAddress, postalAddress);
        webSocketService.broadcastOrganizationsUpdate();
    }
    
    @Transactional(readOnly = true)
    public OrganizationDto findOneWithMinimalCoordinates() {
        Optional<Organization> organization = organizationRepository.findOneOrderedByCoordinatesWithDetails();
        if (!organization.isPresent()) {
            throw new ResourceNotFoundException("Организации не найдены");
        }
        return mapper.toDto(organization.get());
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
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrganizationDto dismissAllEmployees(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с ID " + id + " не найдена"));
        
        organization.setEmployeesCount(0);
        Organization updated = organizationRepository.save(organization);
        return mapper.toDto(updated);
    }
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
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
        webSocketService.broadcastOrganizationsUpdate();
        return mapper.toDto(updated);
    }
    
    private Coordinates getOrCreateCoordinates(OrganizationDto dto) {
        Long id = dto.getCoordinatesId();
        ru.itmo.organization.dto.CoordinatesDto cDto = dto.getCoordinates();

        if (id != null) {
            boolean updated = cDto != null && Boolean.TRUE.equals(cDto.getIsUpdated());
            if (!updated) {
                return coordinatesRepository.getReference(id);
            }

            Coordinates coordinates = coordinatesRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));

            if (cDto == null) {
                throw new IllegalArgumentException("coordinates обязателен при isUpdated=true");
            }

            coordinates.setX(cDto.getX());
            coordinates.setY(cDto.getY());
            return coordinatesRepository.save(coordinates);
        }

        if (cDto == null) {
            throw new IllegalArgumentException("Необходимо указать координаты");
        }
        Coordinates coordinates = mapper.toEntity(cDto);
        return coordinatesRepository.save(coordinates);
    }
    
    private Address getOrCreateAddress(Long addressId, ru.itmo.organization.dto.AddressDto addressDto) {
        if (addressId != null) {
            boolean updated = addressDto != null && Boolean.TRUE.equals(addressDto.getIsUpdated());
            if (!updated) {
                return addressRepository.getReference(addressId);
            }

            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + addressId + " не найден"));

            applyAddressUpdates(address, addressDto);
            return addressRepository.save(address);
        }

        if (addressDto == null) {
            throw new IllegalArgumentException("Необходимо указать адрес");
        }

        Address address = new Address();
        applyAddressUpdates(address, addressDto);
        return addressRepository.save(address);
    }

    private void applyAddressUpdates(Address address, ru.itmo.organization.dto.AddressDto dto) {
        if (dto.getZipCode() != null) {
            if (dto.getZipCode().length() < 7) {
                throw new IllegalArgumentException("Почтовый индекс должен содержать минимум 7 символов");
            }
            address.setZipCode(dto.getZipCode());
        }

        if (dto.getTownId() != null) {
            if (dto.getTown() == null || !Boolean.TRUE.equals(dto.getTown().getIsUpdated())) {
                address.setTown(locationRepository.getReference(dto.getTownId()));
                return;
            }

            Location town = updateOrCreateTown(dto.getTownId(), dto.getTown());
            address.setTown(town);
            return;
        }

        if (dto.getTown() != null) {
            Location town = updateOrCreateTown(dto.getTown().getId(), dto.getTown());
            address.setTown(town);
        } else if (address.getTown() == null) {
            throw new IllegalArgumentException("Необходимо указать город для адреса");
        }
    }

    private Location updateOrCreateTown(Long id, ru.itmo.organization.dto.LocationDto dto) {
        boolean updated = dto != null && Boolean.TRUE.equals(dto.getIsUpdated());

        if (id != null) {
            if (!updated) {
                return locationRepository.getReference(id);
            }

            Location town = locationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));

            town.setName(dto.getName());
            town.setX(dto.getX());
            town.setY(dto.getY());
            town.setZ(dto.getZ());
            return locationRepository.save(town);
        }

        if (dto == null) {
            throw new IllegalArgumentException("Необходимо указать город");
        }

        Location town = mapper.toEntity(dto);
        return locationRepository.save(town);
    }
    
    private void cleanupOrphanedObjects(Coordinates coordinates, Address officialAddress, Address postalAddress) {
        if (coordinates != null && coordinates.getId() != null && !coordinatesRepository.isReferenced(coordinates.getId())) {
            coordinatesRepository.delete(coordinates);
        }

        if (officialAddress != null) {
            deleteAddressIfOrphaned(officialAddress);
        }

        if (postalAddress != null && (officialAddress == null || !postalAddress.getId().equals(officialAddress.getId()))) {
            deleteAddressIfOrphaned(postalAddress);
        }
    }

    private void deleteAddressIfOrphaned(Address address) {
        if (address.getId() == null || addressRepository.isReferenced(address.getId())) {
            return;
        }

        Location town = address.getTown();
        addressRepository.delete(address);

        if (town != null && town.getId() != null && !locationRepository.isReferenced(town.getId())) {
            locationRepository.delete(town);
        }
    }
}
