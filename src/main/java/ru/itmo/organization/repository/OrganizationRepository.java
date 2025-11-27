package ru.itmo.organization.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ru.itmo.organization.model.Organization;
import ru.itmo.organization.model.OrganizationType;

@Repository
public class OrganizationRepository {
    
    private static final Map<String, String> SORT_MAPPING = Map.of(
            "id", "o.id",
            "name", "o.name",
            "fullName", "o.fullName",
            "employeesCount", "o.employeesCount",
            "rating", "o.rating",
            "type", "o.type",
            "annualTurnover", "o.annualTurnover",
            "creationDate", "o.creationDate",
            "coordinates.x", "o.coordinates.x",
            "coordinates.y", "o.coordinates.y"
    );
    
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
    
    public List<Organization> findAllOrderedByCoordinatesWithDetails() {
        return entityManager.createQuery(
                        "SELECT DISTINCT o FROM Organization o " +
                        "LEFT JOIN FETCH o.coordinates " +
                        "LEFT JOIN FETCH o.officialAddress oa " +
                        "LEFT JOIN FETCH oa.town " +
                        "LEFT JOIN FETCH o.postalAddress pa " +
                        "LEFT JOIN FETCH pa.town " +
                        "ORDER BY o.coordinates.x ASC, o.coordinates.y ASC",
                        Organization.class)
                .getResultList();
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
        idQuery.select(idRoot.get("id")).distinct(true);
        applySort(pageable.getSort(), cb, idQuery, idRoot);
        
        List<Long> ids = entityManager.createQuery(idQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        
        List<Organization> content = fetchOrganizationsWithDetails(ids, pageable.getSort());
        return new PageImpl<>(content, pageable, total);
    }
    
    private Predicate buildSearchPredicate(String rawTerm, String rawField, CriteriaBuilder cb, Root<Organization> root) {
        if (rawTerm == null || rawTerm.trim().isEmpty()) {
            return null;
        }
        
        String term = rawTerm.trim().toLowerCase(Locale.ROOT);
        String likePattern = "%" + term + "%";
        String field = rawField == null ? "" : rawField.trim().toLowerCase(Locale.ROOT);
        
        Map<String, Predicate> predicateByField = new HashMap<>();
        predicateByField.put("name", cb.like(cb.lower(root.get("name")), likePattern));
        predicateByField.put("fullname", cb.like(cb.lower(root.get("fullName")), likePattern));
        
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
                jakarta.persistence.criteria.Path<?> path = resolveSortPath(sortOrder.getProperty(), root);
                if (path != null) {
                    orders.add(sortOrder.isDescending() ? cb.desc(path) : cb.asc(path));
                }
            }
        }
        
        if (orders.isEmpty()) {
            orders.add(cb.asc(root.get("id")));
        }
        
        query.orderBy(orders);
    }
    
    private jakarta.persistence.criteria.Path<?> resolveSortPath(String property, Root<Organization> root) {
        return switch (property) {
            case "id", "name", "fullName", "employeesCount", "rating", "type", "annualTurnover", "creationDate" ->
                root.get(property);
            case "coordinates.x" -> root.join("coordinates", JoinType.LEFT).get("x");
            case "coordinates.y" -> root.join("coordinates", JoinType.LEFT).get("y");
            default -> null;
        };
    }
    
    private List<Organization> fetchOrganizationsWithDetails(List<Long> ids, Sort sort) {
        String orderBy = buildOrderClause(sort);
        TypedQuery<Organization> query = entityManager.createQuery(
                        "SELECT DISTINCT o FROM Organization o " +
                        "LEFT JOIN FETCH o.coordinates " +
                        "LEFT JOIN FETCH o.officialAddress oa " +
                        "LEFT JOIN FETCH oa.town " +
                        "LEFT JOIN FETCH o.postalAddress pa " +
                        "LEFT JOIN FETCH pa.town " +
                        "WHERE o.id IN :ids" + orderBy,
                        Organization.class)
                .setParameter("ids", ids);
        
        return query.getResultList();
    }
    
    private String buildOrderClause(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return " ORDER BY o.id ASC";
        }
        
        List<String> clauses = new ArrayList<>();
        for (Sort.Order order : sort) {
            String mapped = SORT_MAPPING.get(order.getProperty());
            if (mapped != null) {
                clauses.add(mapped + (order.isDescending() ? " DESC" : " ASC"));
            }
        }
        
        if (clauses.isEmpty()) {
            clauses.add("o.id ASC");
        }
        
        return " ORDER BY " + String.join(", ", clauses);
    }
}
