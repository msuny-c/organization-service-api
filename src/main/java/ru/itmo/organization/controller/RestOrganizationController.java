package ru.itmo.organization.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.model.OrganizationType;
import ru.itmo.organization.service.AddressService;
import ru.itmo.organization.service.CoordinatesService;
import ru.itmo.organization.service.LocationService;
import ru.itmo.organization.service.OrganizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class RestOrganizationController {
    
    private final OrganizationService organizationService;
    private final LocationService locationService;
    private final CoordinatesService coordinatesService;
    private final AddressService addressService;
    
    @GetMapping
    public ResponseEntity<Page<OrganizationDto>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchField) {
        
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        
        Page<OrganizationDto> organizations;
        if (search != null && !search.trim().isEmpty()) {
            organizations = organizationService.findBySearchTerm(search, searchField, pageable);
        } else {
            organizations = organizationService.findAll(pageable);
        }
        
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable Long id) {
        try {
            OrganizationDto organization = organizationService.findById(id);
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createOrganization(@Valid @RequestBody OrganizationDto organization) {
        try {
            OrganizationDto created = organizationService.create(organization);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationDto organization) {
        try {
            OrganizationDto updated = organizationService.update(id, organization);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        try {
            organizationService.delete(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Организация успешно удалена");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/coordinates")
    public ResponseEntity<?> getCoordinates() {
        return ResponseEntity.ok(coordinatesService.findAll());
    }
    
    @GetMapping("/addresses")
    public ResponseEntity<?> getAddresses() {
        return ResponseEntity.ok(addressService.findAll());
    }
    
    @GetMapping("/locations")
    public ResponseEntity<?> getLocations() {
        return ResponseEntity.ok(locationService.findAll());
    }
    
    @GetMapping("/types")
    public ResponseEntity<OrganizationType[]> getTypes() {
        return ResponseEntity.ok(OrganizationType.values());
    }
}
