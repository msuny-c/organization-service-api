package ru.itmo.organization.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.dto.DeleteRequestDto;
import ru.itmo.organization.mapper.OrganizationMapper;
import ru.itmo.organization.exception.ResourceNotFoundException;
import ru.itmo.organization.repository.LocationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final OrganizationMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<LocationDto> findAll() {
        return locationRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationDto findById(Long id) {
        return locationRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
    }

    public LocationDto create(LocationDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = locationRepository.save(entity);
        return mapper.toDto(saved);
    }

    public LocationDto update(Long id, LocationDto dto) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        existing.setName(dto.getName());
        existing.setX(dto.getX());
        existing.setY(dto.getY());
        existing.setZ(dto.getZ());
        var saved = locationRepository.save(existing);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        if (locationRepository.isReferenced(id)) {
            throw new IllegalStateException("Нельзя удалить локацию, используемую адресами");
        }
        locationRepository.delete(existing);
    }

    public void deleteWithCascade(Long id, DeleteRequestDto request) {
        var existing = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация с ID " + id + " не найдена"));
        if (Boolean.TRUE.equals(request.getCascadeDelete())) {
            List<Long> addressIds = entityManager.createQuery(
                    "SELECT a.id FROM Address a WHERE a.town.id = :locationId", Long.class)
                    .setParameter("locationId", id)
                    .getResultList();
            List<Long> coordinatesIds = entityManager.createQuery(
                    "SELECT DISTINCT o.coordinates.id FROM Organization o WHERE o.postalAddress.id IN :addressIds",
                    Long.class)
                    .setParameter("addressIds", addressIds)
                    .getResultList();

            entityManager.createQuery(
                    "DELETE FROM Organization o WHERE o.postalAddress.id IN (SELECT a.id FROM Address a WHERE a.town.id = :locationId)")
                    .setParameter("locationId", id)
                    .executeUpdate();

            entityManager.createQuery(
                    "UPDATE Organization o SET o.officialAddress = NULL WHERE o.officialAddress.id IN (SELECT a.id FROM Address a WHERE a.town.id = :locationId)")
                    .setParameter("locationId", id)
                    .executeUpdate();

            entityManager.createQuery("DELETE FROM Address a WHERE a.town.id = :locationId")
                    .setParameter("locationId", id)
                    .executeUpdate();

            if (!coordinatesIds.isEmpty()) {
                entityManager.createQuery("DELETE FROM Coordinates c WHERE c.id IN :coordsIds")
                        .setParameter("coordsIds", coordinatesIds)
                        .executeUpdate();
            }

            locationRepository.delete(existing);
        } else {
            if (locationRepository.isReferenced(id)) {
                throw new IllegalStateException(
                        "Локация используется адресами. Укажите cascadeDelete=true для удаления вместе с адресами и организациями.");
            }
            locationRepository.delete(existing);
        }
    }
}
