package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.mapper.ReferenceMapper;
import ru.itmo.organization.repository.CoordinatesRepository;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class CoordinatesService {
    
    private final CoordinatesRepository coordinatesRepository;
    private final ReferenceMapper mapper;
    private final WebSocketService webSocketService;
    
    @Transactional(readOnly = true)
    public List<CoordinatesDto> findAll() {
        return coordinatesRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CoordinatesDto> findAll(Pageable pageable) {
        return coordinatesRepository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public CoordinatesDto findById(Long id) {
        return coordinatesRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
    }

    public CoordinatesDto create(@Valid CoordinatesDto dto) {
        return saveAndBroadcast(mapper.toEntity(dto));
    }

    public CoordinatesDto update(Long id, @Valid CoordinatesDto dto) {
        var existing = coordinatesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
        existing.setX(dto.getX());
        existing.setY(dto.getY());
        return saveAndBroadcast(existing);
    }

    public void delete(Long id) {
        var existing = coordinatesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Координаты с ID " + id + " не найдены"));
        
        if (coordinatesRepository.isReferenced(id)) {
            throw new IllegalStateException("Нельзя удалить координаты, которые используются организациями");
        }
        
        coordinatesRepository.delete(existing);
        webSocketService.broadcastCoordinatesUpdate();
    }

    private CoordinatesDto saveAndBroadcast(ru.itmo.organization.model.Coordinates entity) {
        var saved = coordinatesRepository.save(entity);
        webSocketService.broadcastCoordinatesUpdate();
        return mapper.toDto(saved);
    }
}
