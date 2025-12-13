package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
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

    public Location getReference(Long id) {
        return entityManager.getReference(Location.class, id);
    }

    public boolean isReferenced(Long id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(a.id) FROM Address a WHERE a.town.id = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }

    public boolean existsByName(String name, Long excludeId) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        Long count = entityManager.createQuery(
                        "SELECT COUNT(l.id) FROM Location l WHERE LOWER(l.name) = LOWER(:name) "
                                + "AND (:excludeId IS NULL OR l.id <> :excludeId)",
                        Long.class)
                .setParameter("name", name.trim())
                .setParameter("excludeId", excludeId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Page<Location> findAll(Pageable pageable) {
        List<Location> locations = entityManager.createQuery(
                        "SELECT l FROM Location l ORDER BY l." + pageable.getSort().toString().replace(":", " "),
                        Location.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(
                        "SELECT COUNT(l) FROM Location l",
                        Long.class)
                .getSingleResult();

        return new PageImpl<>(locations, pageable, total);
    }

    public Page<Location> search(String searchTerm, String searchField, Pageable pageable) {
        String jpql = "SELECT l FROM Location l WHERE ";
        String countJpql = "SELECT COUNT(l) FROM Location l WHERE ";
        String whereClause = switch (searchField) {
            case "name" -> "LOWER(l.name) LIKE LOWER(:search)";
            default -> "1=1";
        };
        jpql += whereClause + " ORDER BY l." + pageable.getSort().toString().replace(":", " ");
        countJpql += whereClause;

        List<Location> locations = entityManager.createQuery(jpql, Location.class)
                .setParameter("search", "%" + searchTerm + "%")
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(countJpql, Long.class)
                .setParameter("search", "%" + searchTerm + "%")
                .getSingleResult();

        return new PageImpl<>(locations, pageable, total);
    }
}
