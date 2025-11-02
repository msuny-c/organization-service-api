package com.example.organization.service;

import com.example.organization.dto.CoordinatesDto;
import com.example.organization.exception.ResourceNotFoundException;
import com.example.organization.mapper.OrganizationMapper;
import com.example.organization.model.Coordinates;
import com.example.organization.repository.CoordinatesRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CoordinatesService {
    
    private final CoordinatesRepository coordinatesRepository;
    private final OrganizationMapper mapper;
    
    @Transactional(readOnly = true)
    public List<CoordinatesDto> findAll() {
        return coordinatesRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public CoordinatesDto findById(Long id) {
        Coordinates coordinates = coordinatesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
        return mapper.toDto(coordinates);
    }
    
    public CoordinatesDto create(CoordinatesDto dto) {
        Coordinates coordinates = mapper.toEntity(dto);
        Coordinates saved = coordinatesRepository.save(coordinates);
        return mapper.toDto(saved);
    }
}
