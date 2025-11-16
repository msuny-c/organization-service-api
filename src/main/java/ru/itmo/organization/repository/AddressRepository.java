package ru.itmo.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.itmo.organization.model.Address;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByZipCodeContainingIgnoreCase(String zipCode);
    
    Optional<Address> findByZipCodeAndTownId(String zipCode, Long townId);
    
    @Query("SELECT a FROM Address a WHERE " +
           "(a.zipCode IS NULL OR LOWER(a.zipCode) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<Address> findByZipCodeSearchTerm(@Param("term") String term);
    
    @Query("SELECT a FROM Address a WHERE NOT EXISTS " +
           "(SELECT o FROM Organization o WHERE o.officialAddress = a OR o.postalAddress = a)")
    List<Address> findOrphaned();
}
