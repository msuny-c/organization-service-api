package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
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

    public Coordinates getReference(Long id) {
        return entityManager.getReference(Coordinates.class, id);
    }
    
    public boolean isReferenced(Long id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(o.id) FROM Organization o WHERE o.coordinates.id = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Page<Coordinates> findAll(Pageable pageable) {
        List<Coordinates> coordinates = entityManager.createQuery(
                        "SELECT c FROM Coordinates c ORDER BY c." + pageable.getSort().toString().replace(":", " "),
                        Coordinates.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(
                        "SELECT COUNT(c) FROM Coordinates c",
                        Long.class)
                .getSingleResult();

        return new PageImpl<>(coordinates, pageable, total);
    }
}
