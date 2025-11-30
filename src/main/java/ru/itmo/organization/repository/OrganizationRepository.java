package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.Address;
import ru.itmo.organization.model.Coordinates;
import ru.itmo.organization.model.Organization;
import ru.itmo.organization.model.OrganizationType;

@Repository
public class OrganizationRepository {

    private static final Map<String, Function<Root<Organization>, Path<?>>> SORT_PATHS = Map.ofEntries(
            Map.entry("id", root -> root.get("id")),
            Map.entry("name", root -> root.get("name")),
            Map.entry("fullName", root -> root.get("fullName")),
            Map.entry("employeesCount", root -> root.get("employeesCount")),
            Map.entry("rating", root -> root.get("rating")),
            Map.entry("type", root -> root.get("type")),
            Map.entry("annualTurnover", root -> root.get("annualTurnover")),
            Map.entry("postalAddress.zipCode", root -> root.join("postalAddress", JoinType.LEFT).get("zipCode")),
            Map.entry("postalAddress.town.name", root -> root.join("postalAddress", JoinType.LEFT)
                    .join("town", JoinType.LEFT).get("name")));

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Organization> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Organization.class, id));
    }

    public Optional<Organization> findByIdWithDetails(Long id) {
        List<Organization> result = entityManager.createQuery(
                "SELECT DISTINCT o FROM Organization o " +
                        "LEFT JOIN FETCH o.coordinates " +
                        "LEFT JOIN FETCH o.officialAddress oa " +
                        "LEFT JOIN FETCH oa.town " +
                        "LEFT JOIN FETCH o.postalAddress pa " +
                        "LEFT JOIN FETCH pa.town " +
                        "WHERE o.id = :id",
                Organization.class)
                .setParameter("id", id)
                .getResultList();

        return result.stream().findFirst();
    }

    public Organization save(Organization organization) {
        if (organization.getId() == null) {
            entityManager.persist(organization);
            return organization;
        }
        return entityManager.merge(organization);
    }

    public void delete(Organization organization) {
        if (organization == null) {
            return;
        }
        Organization managed = entityManager.contains(organization)
                ? organization
                : entityManager.merge(organization);
        entityManager.remove(managed);
    }

    public Page<Organization> findAllWithDetails(Pageable pageable) {
        return queryOrganizations(null, null, pageable);
    }

    public Page<Organization> search(String searchTerm, String searchField, Pageable pageable) {
        return queryOrganizations(searchTerm, searchField, pageable);
    }

    public Optional<Organization> findOneOrderedByCoordinatesWithDetails() {
        return entityManager.createQuery(
                "SELECT DISTINCT o FROM Organization o " +
                        "LEFT JOIN FETCH o.coordinates " +
                        "LEFT JOIN FETCH o.officialAddress oa " +
                        "LEFT JOIN FETCH oa.town " +
                        "LEFT JOIN FETCH o.postalAddress pa " +
                        "LEFT JOIN FETCH pa.town " +
                        "ORDER BY o.coordinates.x ASC, o.coordinates.y ASC",
                Organization.class)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }

    public List<Object[]> countByRatingGrouped() {
        return entityManager.createQuery(
                "SELECT o.rating, COUNT(o) FROM Organization o " +
                        "WHERE o.rating IS NOT NULL GROUP BY o.rating ORDER BY o.rating",
                Object[].class)
                .getResultList();
    }

    public long countByType(OrganizationType type) {
        return entityManager.createQuery(
                "SELECT COUNT(o) FROM Organization o WHERE o.type = :type",
                Long.class)
                .setParameter("type", type)
                .getSingleResult();
    }

    private Page<Organization> queryOrganizations(String searchTerm, String searchField, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Organization> countRoot = countQuery.from(Organization.class);
        Predicate countPredicate = buildSearchPredicate(searchTerm, searchField, cb, countRoot);
        if (countPredicate != null) {
            countQuery.where(countPredicate);
        }
        countQuery.select(cb.countDistinct(countRoot));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        CriteriaQuery<Long> idQuery = cb.createQuery(Long.class);
        Root<Organization> idRoot = idQuery.from(Organization.class);
        Predicate idPredicate = buildSearchPredicate(searchTerm, searchField, cb, idRoot);
        if (idPredicate != null) {
            idQuery.where(idPredicate);
        }
        idQuery.select(idRoot.get("id"));
        applySort(pageable.getSort(), cb, idQuery, idRoot);

        List<Long> ids = entityManager.createQuery(idQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<Organization> content = fetchOrganizationsWithDetails(ids);
        return new PageImpl<>(content, pageable, total);
    }

    private Predicate buildSearchPredicate(String rawTerm, String rawField, CriteriaBuilder cb,
            Root<Organization> root) {
        if (rawTerm == null || rawTerm.trim().isEmpty()) {
            return null;
        }

        String term = rawTerm.trim().toLowerCase(Locale.ROOT);
        String likePattern = "%" + term + "%";
        String field = rawField == null ? "" : rawField.trim().toLowerCase(Locale.ROOT);

        Join<Organization, ?> postalJoin = root.join("postalAddress", JoinType.LEFT);
        Join<?, ?> postalTownJoin = postalJoin.join("town", JoinType.LEFT);

        Map<String, Predicate> predicateByField = new HashMap<>();
        predicateByField.put("name", cb.like(cb.lower(root.get("name")), likePattern));
        predicateByField.put("fullname", cb.like(cb.lower(root.get("fullName")), likePattern));
        predicateByField.put("postaladdress.zipcode", cb.like(cb.lower(postalJoin.get("zipCode")), likePattern));
        predicateByField.put("postaladdress.town.name", cb.like(cb.lower(postalTownJoin.get("name")), likePattern));

        if (!field.isEmpty() && !"all".equals(field)) {
            Predicate selected = predicateByField.get(field.toLowerCase(Locale.ROOT));
            return selected != null ? selected : cb.or(predicateByField.values().toArray(new Predicate[0]));
        }

        return cb.or(predicateByField.values().toArray(new Predicate[0]));
    }

    private void applySort(Sort sort, CriteriaBuilder cb, CriteriaQuery<?> query, Root<Organization> root) {
        List<Order> orders = new ArrayList<>();

        if (sort == null || sort.isUnsorted()) {
            orders.add(cb.asc(root.get("id")));
        } else {
            for (Sort.Order sortOrder : sort) {
                Path<?> path = resolveSortPath(sortOrder.getProperty(), root);
                if (path != null) {
                    orders.add(sortOrder.isDescending() ? cb.desc(path) : cb.asc(path));
                }
            }
        }

        boolean hasIdSort = orders.stream()
                .anyMatch(order -> order.getExpression().equals(root.get("id")));
        if (!hasIdSort) {
            orders.add(cb.asc(root.get("id")));
        }

        query.orderBy(orders);
    }

    private Path<?> resolveSortPath(String property, Root<Organization> root) {
        Function<Root<Organization>, Path<?>> builder = SORT_PATHS.get(property);
        if (builder == null) {
            return null;
        }
        return builder.apply(root);
    }

    private List<Organization> fetchOrganizationsWithDetails(List<Long> ids) {
        TypedQuery<Organization> query = entityManager.createQuery(
                "SELECT DISTINCT o FROM Organization o " +
                        "LEFT JOIN FETCH o.coordinates " +
                        "LEFT JOIN FETCH o.officialAddress oa " +
                        "LEFT JOIN FETCH oa.town " +
                        "LEFT JOIN FETCH o.postalAddress pa " +
                        "LEFT JOIN FETCH pa.town " +
                        "WHERE o.id IN :ids",
                Organization.class)
                .setParameter("ids", ids);

        List<Organization> content = query.getResultList();
        Map<Long, Integer> positions = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            positions.put(ids.get(i), i);
        }
        content.sort(Comparator.comparingInt(o -> positions.getOrDefault(o.getId(), Integer.MAX_VALUE)));
        return content;
    }

    public void deleteAllByCoordinatesId(Long coordinatesId) {
        entityManager.createQuery(
                "DELETE FROM Organization o WHERE o.coordinates.id = :coordinatesId")
                .setParameter("coordinatesId", coordinatesId)
                .executeUpdate();
    }

    public void deleteAllByOfficialAddressId(Long addressId) {
        List<Long> ids = entityManager.createQuery(
                "SELECT o.id FROM Organization o WHERE o.officialAddress.id = :addressId",
                Long.class)
                .setParameter("addressId", addressId)
                .getResultList();
        for (Long id : ids) {
            var org = entityManager.find(Organization.class, id);
            if (org != null) {
                entityManager.remove(org);
            }
        }
    }

    public void deleteAllByPostalAddressId(Long addressId) {
        List<Long> ids = entityManager.createQuery(
                "SELECT o.id FROM Organization o WHERE o.postalAddress.id = :addressId",
                Long.class)
                .setParameter("addressId", addressId)
                .getResultList();
        for (Long id : ids) {
            var org = entityManager.find(Organization.class, id);
            if (org != null) {
                entityManager.remove(org);
            }
        }
    }

    public void deleteAllByLocationTownId(Long locationId) {
        List<Long> ids = entityManager.createQuery(
                "SELECT o.id FROM Organization o WHERE (o.postalAddress.town.id = :locationId) OR (o.officialAddress.town.id = :locationId)",
                Long.class)
                .setParameter("locationId", locationId)
                .getResultList();
        for (Long id : ids) {
            var org = entityManager.find(Organization.class, id);
            if (org != null) {
                entityManager.remove(org);
            }
        }
    }

    public List<Long> findAddressIdsByCoordinatesId(Long coordinatesId) {
        return entityManager.createQuery(
                "SELECT DISTINCT o.postalAddress.id FROM Organization o WHERE o.coordinates.id = :coordsId", Long.class)
                .setParameter("coordsId", coordinatesId)
                .getResultList();
    }

    public List<Long> findLocationIdsByAddressIds(List<Long> addressIds) {
        if (addressIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                "SELECT DISTINCT a.town.id FROM Address a WHERE a.id IN :addressIds", Long.class)
                .setParameter("addressIds", addressIds)
                .getResultList();
    }

    public void deleteAddressesByIds(List<Long> addressIds) {
        for (Long id : addressIds) {
            var address = entityManager.find(Address.class, id);
            if (address != null) {
                entityManager.remove(address);
            }
        }
    }

    public void deleteLocationsByIds(List<Long> locationIds) {
        for (Long id : locationIds) {
            entityManager.createQuery(
                    "DELETE FROM Location l WHERE l.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
        }
    }

    public List<Long> findAddressIdsByLocationId(Long locationId) {
        return entityManager.createQuery(
                "SELECT a.id FROM Address a WHERE a.town.id = :locationId", Long.class)
                .setParameter("locationId", locationId)
                .getResultList();
    }

    public List<Long> findCoordinatesIdsByAddressIds(List<Long> addressIds) {
        if (addressIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                "SELECT DISTINCT o.coordinates.id FROM Organization o WHERE o.postalAddress.id IN :addressIds", Long.class)
                .setParameter("addressIds", addressIds)
                .getResultList();
    }

    public void deleteOrganizationsByLocationId(Long locationId) {
        List<Long> ids = entityManager.createQuery(
                "SELECT o.id FROM Organization o WHERE o.postalAddress.town.id = :locationId OR o.officialAddress.town.id = :locationId",
                Long.class)
                .setParameter("locationId", locationId)
                .getResultList();
        for (Long id : ids) {
            var org = entityManager.find(Organization.class, id);
            if (org != null) {
                entityManager.remove(org);
            }
        }
    }

    public void deleteCoordinatesByIds(List<Long> coordinatesIds) {
        for (Long id : coordinatesIds) {
            var coordinates = entityManager.find(Coordinates.class, id);
            if (coordinates != null) {
                entityManager.remove(coordinates);
            }
        }
    }
}
