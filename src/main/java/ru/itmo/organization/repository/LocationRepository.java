package ru.itmo.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.itmo.organization.model.Location;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    
    List<Location> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT l FROM Location l WHERE " +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Location> findBySearchTerm(@Param("term") String term);
    
    @Query("SELECT l FROM Location l WHERE NOT EXISTS " +
           "(SELECT a FROM Address a WHERE a.town = l)")
    List<Location> findOrphaned();
    
    boolean existsById(Long id);
}
