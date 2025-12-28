package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.ImportOperation;

@Repository
public class ImportOperationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public ImportOperation save(ImportOperation operation) {
        if (operation.getId() == null) {
            entityManager.persist(operation);
            return operation;
        }
        return entityManager.merge(operation);
    }

    public Optional<ImportOperation> findById(Long id) {
        return Optional.ofNullable(entityManager.find(ImportOperation.class, id));
    }

    public List<ImportOperation> findAll() {
        return entityManager.createQuery(
                        "SELECT o FROM ImportOperation o ORDER BY o.startedAt DESC",
                        ImportOperation.class)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
    }

    public List<ImportOperation> findAllForUser(String username) {
        return entityManager.createQuery(
                        "SELECT o FROM ImportOperation o WHERE o.username = :username ORDER BY o.startedAt DESC",
                        ImportOperation.class)
                .setParameter("username", username)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
    }
}
