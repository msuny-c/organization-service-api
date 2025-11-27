package ru.itmo.organization.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.mapper.OrganizationMapper;
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
}

