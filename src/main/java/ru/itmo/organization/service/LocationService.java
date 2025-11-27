package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.repository.LocationRepository;

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
        return locationRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
