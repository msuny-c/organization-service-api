package ru.itmo.organization.controller;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.model.OrganizationType;
import ru.itmo.organization.service.OrganizationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/operations")
@RequiredArgsConstructor
public class RestOperationsController {
    
    private final OrganizationService organizationService;
    
    @PostMapping("/minimal-coordinates")
    public ResponseEntity<?> findMinimalCoordinates() {
        try {
            OrganizationDto organization = organizationService.findOneWithMinimalCoordinates();
            return ResponseEntity.ok(organization);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/group-by-rating")
    public ResponseEntity<?> groupByRating() {
        try {
            Map<Integer, Long> ratingGroups = organizationService.groupByRating();
            return ResponseEntity.ok(ratingGroups);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/count-by-type")
    public ResponseEntity<?> countByType(@RequestParam OrganizationType type) {
        try {
            long count = organizationService.countByType(type);
            Map<String, Object> result = new HashMap<>();
            result.put("type", type);
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/dismiss-employees")
    public ResponseEntity<?> dismissAllEmployees(@RequestParam Long organizationId) {
        try {
            OrganizationDto updated = organizationService.dismissAllEmployees(organizationId);
            Map<String, Object> result = new HashMap<>();
            result.put("organization", updated);
            result.put("message", "Все сотрудники организации \"" + updated.getName() + "\" уволены");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/absorb")
    public ResponseEntity<?> absorb(@RequestParam Long absorbingId, @RequestParam Long absorbedId) {
        try {
            OrganizationDto absorbing = organizationService.absorb(absorbingId, absorbedId);
            Map<String, Object> result = new HashMap<>();
            result.put("organization", absorbing);
            result.put("message", "Организация успешно поглощена. Новое количество сотрудников: " + absorbing.getEmployeesCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
