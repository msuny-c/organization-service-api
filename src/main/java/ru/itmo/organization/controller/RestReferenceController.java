package ru.itmo.organization.controller;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> getCoordinates() {
        return ResponseEntity.ok(coordinatesService.findAll());
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
        coordinatesService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Координаты успешно удалены");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/addresses")
    public ResponseEntity<?> getAddresses() {
        return ResponseEntity.ok(addressService.findAll());
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
        addressService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Адрес успешно удален");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/locations")
    public ResponseEntity<?> getLocations() {
        return ResponseEntity.ok(locationService.findAll());
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
        LocationDto updated = locationService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        locationService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Локация успешно удалена");
        return ResponseEntity.ok(response);
    }
}
