package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.repository.LocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;
    private final WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return locationRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> findAll(Pageable pageable) {
        return locationRepository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> findBySearchTerm(String searchTerm, String searchField, Pageable pageable) {
        return locationRepository.search(searchTerm, searchField, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public LocationDto findById(Long id) {
        return locationRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
    }

    public LocationDto create(LocationDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = locationRepository.save(entity);
        webSocketService.broadcastLocationsUpdate();
        return mapper.toDto(saved);
    }

    public LocationDto update(Long id, LocationDto dto) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        existing.setX(dto.getX());
        existing.setY(dto.getY());
        existing.setZ(dto.getZ());
        var saved = locationRepository.save(existing);
        webSocketService.broadcastLocationsUpdate();
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        
        if (locationRepository.isReferenced(id)) {
            throw new IllegalStateException("Нельзя удалить локацию, которая используется адресами");
        }
        
        locationRepository.delete(existing);
        webSocketService.broadcastLocationsUpdate();
    }
}
