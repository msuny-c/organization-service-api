package com.example.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    
    private Long id;
    
    @NotNull(message = "Координата X не может быть null")
    private Integer x;
    
    @NotNull(message = "Координата Y не может быть null")
    private Integer y;
    
    @NotNull(message = "Координата Z не может быть null")
    private Double z;
    
    @NotNull(message = "Имя не может быть null")
    private String name;
}
