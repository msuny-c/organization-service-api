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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchField) {

        Page<OrganizationDto> organizations = (search != null && !search.isBlank())
                ? organizationService.findBySearchTerm(search, searchField, pageable)
                : organizationService.findAll(pageable);

        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable Long id) {
        OrganizationDto organization = organizationService.findById(id);
        return ResponseEntity.ok(organization);
    }

    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody OrganizationDto organization) {
        OrganizationDto created = organizationService.create(organization);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationDto organization) {
        organization.setId(id);
        OrganizationDto updated = organizationService.update(id, organization);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        organizationService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Организация успешно удалена");
        return ResponseEntity.ok(response);
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
