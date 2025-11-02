package com.example.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatesDto {
    
    private Long id;
    
    @NotNull(message = "Координата X не может быть null")
    @Max(value = 882, message = "Координата X не может быть больше 882")
    private Double x;
    
    @NotNull(message = "Координата Y не может быть null")
    @Min(value = -539, message = "Координата Y должна быть больше -540")
    private Long y;
}
