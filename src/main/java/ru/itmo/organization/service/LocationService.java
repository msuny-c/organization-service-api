package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.TransactionSystemException;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.mapper.ReferenceMapper;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.repository.LocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import ru.itmo.organization.validation.UniqueLocation;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final ReferenceMapper mapper;
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

    @Retryable(
            retryFor = {PessimisticLockingFailureException.class, TransactionSystemException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 1000, random = true))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LocationDto create(@Valid @UniqueLocation LocationDto dto) {
        return saveAndBroadcast(mapper.toEntity(dto));
    }

    @Retryable(
            retryFor = {PessimisticLockingFailureException.class, TransactionSystemException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 1000, random = true))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LocationDto update(Long id, @Valid @UniqueLocation LocationDto dto) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        existing.setX(dto.getX());
        existing.setY(dto.getY());
        existing.setZ(dto.getZ());
        return saveAndBroadcast(existing);
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

    private LocationDto saveAndBroadcast(ru.itmo.organization.model.Location entity) {
        var saved = locationRepository.save(entity);
        webSocketService.broadcastLocationsUpdate();
        return mapper.toDto(saved);
    }
}
