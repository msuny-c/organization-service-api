package com.example.organization.service;

import com.example.organization.dto.AddressDto;
import com.example.organization.exception.ResourceNotFoundException;
import com.example.organization.mapper.OrganizationMapper;
import com.example.organization.model.Address;
import com.example.organization.model.Location;
import com.example.organization.repository.AddressRepository;
import com.example.organization.repository.LocationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository addressRepository;
    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;
    
    @Transactional(readOnly = true)
    public List<AddressDto> findAll() {
        return addressRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AddressDto findById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Адрес с ID " + id + " не найден"));
        return mapper.toDto(address);
    }
    
    @Transactional(readOnly = true)
    public List<AddressDto> findBySearchTerm(String searchTerm) {
        return addressRepository.findByZipCodeSearchTerm(searchTerm)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    public AddressDto create(AddressDto dto) {
        Address address = mapper.toEntity(dto);
        
        if (dto.getTownId() != null) {
            Location town = locationRepository.findById(dto.getTownId())
                    .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + dto.getTownId() + " не найдена"));
            address.setTown(town);
        } else if (dto.getTown() != null) {
            Location town = mapper.toEntity(dto.getTown());
            town = locationRepository.save(town);
            address.setTown(town);
        } else {
            throw new IllegalArgumentException("Необходимо указать город для адреса");
        }
        
        Address saved = addressRepository.save(address);
        return mapper.toDto(saved);
    }
}
