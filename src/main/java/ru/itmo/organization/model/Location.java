package ru.itmo.organization.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "ru.itmo.organization.model.Location")
public class Location {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false)
    private Long x;
    
    @NotNull
    @Column(nullable = false)
    private Long y;
    
    @NotNull
    @Column(nullable = false)
    private Double z;
    
    @NotNull
    @Column(nullable = false)
    private String name;
}
