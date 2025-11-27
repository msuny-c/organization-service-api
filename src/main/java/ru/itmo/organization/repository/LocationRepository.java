package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.Location;

@Repository
public class LocationRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Location> findAll() {
        return entityManager.createQuery("SELECT l FROM Location l", Location.class)
                .getResultList();
    }
    
    public Optional<Location> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Location.class, id));
    }
    
    public Location save(Location location) {
        if (location.getId() == null) {
            entityManager.persist(location);
            return location;
        }
        return entityManager.merge(location);
    }
    
    public void delete(Location location) {
        if (location == null) {
            return;
        }
        Location managed = entityManager.contains(location) ? location : entityManager.merge(location);
        entityManager.remove(managed);
    }
    
    public List<Location> findOrphaned() {
        return entityManager.createQuery(
                        "SELECT l FROM Location l WHERE NOT EXISTS " +
                        "(SELECT a FROM Address a WHERE a.town = l)",
                        Location.class)
                .getResultList();
    }
}
