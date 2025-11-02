package com.example.organization.repository;

import com.example.organization.model.Coordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
    
    @Query("SELECT c FROM Coordinates c WHERE NOT EXISTS " +
           "(SELECT o FROM Organization o WHERE o.coordinates = c)")
    java.util.List<Coordinates> findOrphaned();
    
    @Query("SELECT c FROM Coordinates c ORDER BY c.x ASC, c.y ASC")
    java.util.List<Coordinates> findAllOrderedByXThenY();
    
    Optional<Coordinates> findByXAndY(Long x, Long y);
}
