package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.UserAccount;

@Repository
public class UserAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<UserAccount> findByUsername(String username) {
        return entityManager.createQuery(
                        "SELECT u FROM UserAccount u WHERE LOWER(u.username) = LOWER(:username)",
                        UserAccount.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u.id) FROM UserAccount u WHERE LOWER(u.username) = LOWER(:username)",
                        Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count != null && count > 0;
    }

    public UserAccount save(UserAccount user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }
}
