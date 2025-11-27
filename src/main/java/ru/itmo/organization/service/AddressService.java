package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.repository.AddressRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository repository;
    private final OrganizationMapper mapper;
    
    @Transactional(readOnly = true)
    public List<AddressDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
