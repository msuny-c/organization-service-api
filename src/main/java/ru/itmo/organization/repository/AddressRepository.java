package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.Address;

@Repository
public class AddressRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Address> findAll() {
        return entityManager.createQuery(
                        "SELECT DISTINCT a FROM Address a " +
                        "LEFT JOIN FETCH a.town",
                        Address.class)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
    }
    
    public Optional<Address> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Address.class, id));
    }
    
    public Address save(Address address) {
        if (address.getId() == null) {
            entityManager.persist(address);
            return address;
        }
        return entityManager.merge(address);
    }
    
    public void delete(Address address) {
        if (address == null) {
            return;
        }
        Address managed = entityManager.contains(address) ? address : entityManager.merge(address);
        entityManager.remove(managed);
    }

    public Address getReference(Long id) {
        return entityManager.getReference(Address.class, id);
    }

    public boolean isReferenced(Long id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(o.id) FROM Organization o WHERE o.officialAddress.id = :id OR o.postalAddress.id = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Page<Address> findAll(Pageable pageable) {
        List<Address> addresses = entityManager.createQuery(
                        "SELECT DISTINCT a FROM Address a " +
                        "LEFT JOIN FETCH a.town",
                        Address.class)
                .setHint("org.hibernate.cacheable", true)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT a) FROM Address a",
                        Long.class)
                .setHint("org.hibernate.cacheable", true)
                .getSingleResult();

        return new PageImpl<>(addresses, pageable, total);
    }

    public Page<Address> search(String searchTerm, String searchField, Pageable pageable) {
        String jpql = "SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.town WHERE ";
        String countJpql = "SELECT COUNT(DISTINCT a) FROM Address a WHERE ";
        String whereClause = switch (searchField) {
            case "zipCode" -> "LOWER(a.zipCode) LIKE LOWER(:search)";
            case "town.name" -> "LOWER(a.town.name) LIKE LOWER(:search)";
            default -> "1=1";
        };
        jpql += whereClause;
        countJpql += whereClause;

        List<Address> addresses = entityManager.createQuery(jpql, Address.class)
                .setParameter("search", "%" + searchTerm + "%")
                .setHint("org.hibernate.cacheable", true)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(countJpql, Long.class)
                .setParameter("search", "%" + searchTerm + "%")
                .setHint("org.hibernate.cacheable", true)
                .getSingleResult();

        return new PageImpl<>(addresses, pageable, total);
    }
}
