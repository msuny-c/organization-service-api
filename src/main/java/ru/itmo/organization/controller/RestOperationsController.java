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
    
    @GetMapping("/minimal-coordinates")
    public ResponseEntity<OrganizationDto> findMinimalCoordinates() {
        OrganizationDto organization = organizationService.findOneWithMinimalCoordinates();
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping("/group-by-rating")
    public ResponseEntity<Map<Integer, Long>> groupByRating() {
        Map<Integer, Long> ratingGroups = organizationService.groupByRating();
        return ResponseEntity.ok(ratingGroups);
    }
    
    @GetMapping("/count-by-type")
    public ResponseEntity<Map<String, Object>> countByType(@RequestParam OrganizationType type) {
        long count = organizationService.countByType(type);
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("count", count);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/dismiss-employees")
    public ResponseEntity<Map<String, Object>> dismissAllEmployees(@RequestParam Long organizationId) {
        OrganizationDto updated = organizationService.dismissAllEmployees(organizationId);
        Map<String, Object> result = new HashMap<>();
        result.put("organization", updated);
        result.put("message", "Все сотрудники организации \"" + updated.getName() + "\" уволены");
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/absorb")
    public ResponseEntity<Map<String, Object>> absorb(@RequestParam Long absorbingId, @RequestParam Long absorbedId) {
        OrganizationDto absorbing = organizationService.absorb(absorbingId, absorbedId);
        Map<String, Object> result = new HashMap<>();
        result.put("organization", absorbing);
        result.put("message", "Организация успешно поглощена. Новое количество сотрудников: " + absorbing.getEmployeesCount());
        return ResponseEntity.ok(result);
    }
}
