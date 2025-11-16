package ru.itmo.organization.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location")
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
