package com.example.organization.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    
    private Long id;
    
    @Size(min = 7, message = "Почтовый индекс должен содержать минимум 7 символов")
    private String zipCode;
    
    private Long townId;
    
    private LocationDto town;
}
