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
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.mapper.ReferenceMapper;
import ru.itmo.organization.repository.AddressRepository;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository repository;
    private final ReferenceMapper mapper;
    private final WebSocketService webSocketService;
    
    @Transactional(readOnly = true)
    public List<AddressDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AddressDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AddressDto> findBySearchTerm(String searchTerm, String searchField, Pageable pageable) {
        return repository.search(searchTerm, searchField, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public AddressDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + id + " не найден"));
    }

    public AddressDto create(@Valid AddressDto dto) {
        return saveAndBroadcast(mapper.toEntity(dto));
    }

    public AddressDto update(Long id, @Valid AddressDto dto) {
        var existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + id + " не найден"));
        existing.setZipCode(dto.getZipCode());
        return saveAndBroadcast(existing);
    }

    public void delete(Long id) {
        var existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + id + " не найден"));
        
        if (repository.isReferenced(id)) {
            throw new IllegalStateException("Нельзя удалить адрес, который используется организациями");
        }
        
        repository.delete(existing);
        webSocketService.broadcastAddressesUpdate();
    }

    private AddressDto saveAndBroadcast(ru.itmo.organization.model.Address entity) {
        var saved = repository.save(entity);
        webSocketService.broadcastAddressesUpdate();
        return mapper.toDto(saved);
    }
}
