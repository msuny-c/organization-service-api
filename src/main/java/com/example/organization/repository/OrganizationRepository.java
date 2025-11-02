package com.example.organization.repository;

import com.example.organization.model.Organization;
import com.example.organization.model.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<Organization> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    
    @Query("SELECT o FROM Organization o " +
           "LEFT JOIN FETCH o.coordinates " +
           "LEFT JOIN FETCH o.officialAddress oa " +
           "LEFT JOIN FETCH oa.town " +
           "LEFT JOIN FETCH o.postalAddress pa " +
           "LEFT JOIN FETCH pa.town " +
           "WHERE o.id = :id")
    Optional<Organization> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT o FROM Organization o " +
           "LEFT JOIN FETCH o.coordinates " +
           "LEFT JOIN FETCH o.officialAddress oa " +
           "LEFT JOIN FETCH oa.town " +
           "LEFT JOIN FETCH o.postalAddress pa " +
           "LEFT JOIN FETCH pa.town")
    Page<Organization> findAllWithDetails(Pageable pageable);
    
    @Query("SELECT o FROM Organization o WHERE " +
           "LOWER(o.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "(o.fullName IS NOT NULL AND LOWER(o.fullName) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<Organization> findBySearchTerm(@Param("term") String term, Pageable pageable);
    
    @Query("SELECT o.rating, COUNT(o) FROM Organization o WHERE o.rating IS NOT NULL GROUP BY o.rating ORDER BY o.rating")
    List<Object[]> countByRatingGrouped();
    
    long countByType(OrganizationType type);
    
    @Query("SELECT o FROM Organization o " +
           "ORDER BY o.coordinates.x ASC, o.coordinates.y ASC")
    List<Organization> findAllOrderedByCoordinates();
    
    @Query("SELECT o FROM Organization o " +
           "LEFT JOIN FETCH o.coordinates " +
           "LEFT JOIN FETCH o.officialAddress oa " +
           "LEFT JOIN FETCH oa.town " +
           "LEFT JOIN FETCH o.postalAddress pa " +
           "LEFT JOIN FETCH pa.town " +
           "ORDER BY o.coordinates.x ASC, o.coordinates.y ASC")
    List<Organization> findAllOrderedByCoordinatesWithDetails();
}
