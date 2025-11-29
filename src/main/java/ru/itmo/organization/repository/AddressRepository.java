package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
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
}
