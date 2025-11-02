package com.example.organization.service;

import com.example.organization.dto.LocationDto;
import com.example.organization.exception.ResourceNotFoundException;
import com.example.organization.mapper.OrganizationMapper;
import com.example.organization.model.Location;
import com.example.organization.repository.LocationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {
    
    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;
    
    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public LocationDto findById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        return mapper.toDto(location);
    }
    
    @Transactional(readOnly = true)
    public List<LocationDto> findBySearchTerm(String searchTerm) {
        return locationRepository.findBySearchTerm(searchTerm)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    public LocationDto create(LocationDto dto) {
        Location location = mapper.toEntity(dto);
        Location saved = locationRepository.save(location);
        return mapper.toDto(saved);
    }
}
