package ru.itmo.organization.dto;

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
    private Long x;
    
    @NotNull(message = "Координата Y не может быть null")
    private Long y;
}
