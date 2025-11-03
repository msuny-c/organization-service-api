package com.example.organization.repository;

import com.example.organization.model.Address;
import com.example.organization.model.Location;
import com.example.organization.model.Organization;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationSpecifications {
    
    private OrganizationSpecifications() {
    }
    
    public static Specification<Organization> searchByTerm(String rawTerm, String rawField) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            boolean isCountQuery = resultType == Long.class || resultType == long.class;
            
            if (!isCountQuery) {
                query.distinct(true);
            }
            
            if (rawTerm == null || rawTerm.trim().isEmpty()) {
                return cb.conjunction();
            }
            
            String term = rawTerm.trim().toLowerCase(Locale.ROOT);
            String likePattern = "%" + term + "%";
            String field = rawField == null ? "" : rawField.trim().toLowerCase(Locale.ROOT);
            
            Join<Organization, Address> officialAddress = root.join("officialAddress", JoinType.LEFT);
            Join<Address, Location> officialTown = officialAddress.join("town", JoinType.LEFT);
            Join<Organization, Address> postalAddress = root.join("postalAddress", JoinType.LEFT);
            Join<Address, Location> postalTown = postalAddress.join("town", JoinType.LEFT);
            
            List<Predicate> allPredicates = new ArrayList<>();
            Map<String, Predicate> predicateByField = new HashMap<>();
            
            Predicate byName = cb.like(cb.lower(root.get("name")), likePattern);
            predicateByField.put("name", byName);
            allPredicates.add(byName);
            
            Predicate byFullName = cb.like(cb.lower(root.get("fullName")), likePattern);
            predicateByField.put("fullname", byFullName);
            allPredicates.add(byFullName);
            
            Predicate byOfficialZip = cb.like(cb.lower(officialAddress.get("zipCode")), likePattern);
            predicateByField.put("officialzip", byOfficialZip);
            allPredicates.add(byOfficialZip);
            
            Predicate byPostalZip = cb.like(cb.lower(postalAddress.get("zipCode")), likePattern);
            predicateByField.put("postalzip", byPostalZip);
            allPredicates.add(byPostalZip);
            
            Predicate byOfficialTown = cb.like(cb.lower(officialTown.get("name")), likePattern);
            predicateByField.put("officialtown", byOfficialTown);
            allPredicates.add(byOfficialTown);
            
            Predicate byPostalTown = cb.like(cb.lower(postalTown.get("name")), likePattern);
            predicateByField.put("postaltown", byPostalTown);
            allPredicates.add(byPostalTown);
            
            if (!field.isEmpty() && !"all".equals(field)) {
                Predicate selected = predicateByField.get(field);
                return selected != null ? selected : cb.or(allPredicates.toArray(new Predicate[0]));
            }
            
            return cb.or(allPredicates.toArray(new Predicate[0]));
        };
    }
}
