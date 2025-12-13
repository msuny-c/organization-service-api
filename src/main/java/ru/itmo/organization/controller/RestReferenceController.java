package ru.itmo.organization.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.organization.dto.AddressDto;
import ru.itmo.organization.dto.CoordinatesDto;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.service.AddressService;
import ru.itmo.organization.service.CoordinatesService;
import ru.itmo.organization.service.LocationService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RestReferenceController {

    private final CoordinatesService coordinatesService;
    private final AddressService addressService;
    private final LocationService locationService;

    @GetMapping("/coordinates")
    public ResponseEntity<Page<CoordinatesDto>> getCoordinates(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(coordinatesService.findAll(pageable));
    }

    @GetMapping("/coordinates/{id}")
    public ResponseEntity<CoordinatesDto> getCoordinatesById(@PathVariable Long id) {
        return ResponseEntity.ok(coordinatesService.findById(id));
    }

    @PostMapping("/coordinates")
    public ResponseEntity<CoordinatesDto> createCoordinates(@Valid @RequestBody CoordinatesDto dto) {
        CoordinatesDto created = coordinatesService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/coordinates/{id}")
    public ResponseEntity<CoordinatesDto> updateCoordinates(@PathVariable Long id, @Valid @RequestBody CoordinatesDto dto) {
        CoordinatesDto updated = coordinatesService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/coordinates/{id}")
    public ResponseEntity<?> deleteCoordinates(@PathVariable Long id) {
        try {
            coordinatesService.delete(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Координаты успешно удалены");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/addresses")
    public ResponseEntity<Page<AddressDto>> getAddresses(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchField) {
        Page<AddressDto> addresses = (search != null && !search.isBlank())
                ? addressService.findBySearchTerm(search, searchField, pageable)
                : addressService.findAll(pageable);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/addresses/{id}")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto dto) {
        AddressDto created = addressService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto dto) {
        AddressDto updated = addressService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        try {
            addressService.delete(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Адрес успешно удален");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<Page<LocationDto>> getLocations(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchField) {
        Page<LocationDto> locations = (search != null && !search.isBlank())
                ? locationService.findBySearchTerm(search, searchField, pageable)
                : locationService.findAll(pageable);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationDto> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PostMapping("/locations")
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody LocationDto dto) {
        LocationDto created = locationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<LocationDto> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDto dto) {
        dto.setId(id);
        LocationDto updated = locationService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        try {
            locationService.delete(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Локация успешно удалена");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
