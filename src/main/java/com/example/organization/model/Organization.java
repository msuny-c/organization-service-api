package com.example.organization.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization")
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;
    
    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "official_address_id")
    private Address officialAddress;
    
    @Positive
    @Column(name = "annual_turnover")
    private Long annualTurnover;
    
    @NotNull
    @Min(0)
    @Column(name = "employees_count", nullable = false)
    private Integer employeesCount;
    
    @Positive
    @Column
    private Integer rating;
    
    @Pattern(regexp = "^(?!\\s*$).+", message = "Полное название не может быть пустой строкой")
    @Column(name = "full_name")
    private String fullName;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType type;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postal_address_id", nullable = false)
    private Address postalAddress;
    
    @Version
    private Long version;
    
    @PrePersist
    public void onCreate() {
        this.creationDate = LocalDate.now();
    }
}
