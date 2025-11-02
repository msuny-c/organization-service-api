package com.example.organization.dto;

import com.example.organization.model.OrganizationType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationDto {
    
    private Long id;
    
    @NotBlank(message = "Название не может быть пустым")
    private String name;
    
    private Long coordinatesId;
    private CoordinatesDto coordinates;
    
    private LocalDate creationDate;
    
    private Long officialAddressId;
    private AddressDto officialAddress;
    
    @Positive(message = "Годовой оборот должен быть положительным")
    private Integer annualTurnover;
    
    @Min(value = 0, message = "Количество сотрудников не может быть отрицательным")
    private Integer employeesCount;
    
    @Positive(message = "Рейтинг должен быть положительным")
    @NotNull(message = "Рейтинг не может быть null")
    private Long rating;
    
    @Pattern(regexp = "^$|^(?!\\s*$).+", message = "Полное название не может быть пустой строкой")
    private String fullName;
    
    private OrganizationType type;
    
    private Long postalAddressId;
    private AddressDto postalAddress;
    
    private Boolean reusePostalAddressAsOfficial;
    
    private Long version;
}
