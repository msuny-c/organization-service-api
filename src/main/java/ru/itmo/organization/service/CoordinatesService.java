package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.repository.CoordinatesRepository;

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
        return coordinatesRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CoordinatesDto findById(Long id) {
        return coordinatesRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
    }

    public CoordinatesDto create(CoordinatesDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = coordinatesRepository.save(entity);
        return mapper.toDto(saved);
    }

    public CoordinatesDto update(Long id, CoordinatesDto dto) {
        var existing = coordinatesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
        existing.setX(dto.getX());
        existing.setY(dto.getY());
        var saved = coordinatesRepository.save(existing);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        var existing = coordinatesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
        coordinatesRepository.delete(existing);
    }
}

