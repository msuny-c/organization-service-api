package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.Coordinates;

@Repository
public class CoordinatesRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Coordinates> findAll() {
        return entityManager.createQuery("SELECT c FROM Coordinates c", Coordinates.class)
                .getResultList();
    }
    
    public Optional<Coordinates> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Coordinates.class, id));
    }
    
    public Coordinates save(Coordinates coordinates) {
        if (coordinates.getId() == null) {
            entityManager.persist(coordinates);
            return coordinates;
        }
        return entityManager.merge(coordinates);
    }
    
    public void delete(Coordinates coordinates) {
        if (coordinates == null) {
            return;
        }
        Coordinates managed = entityManager.contains(coordinates) ? coordinates : entityManager.merge(coordinates);
        entityManager.remove(managed);
    }
    
    public boolean isReferenced(Long id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(o.id) FROM Organization o WHERE o.coordinates.id = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }
}
