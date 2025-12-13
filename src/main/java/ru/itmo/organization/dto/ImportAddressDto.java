package ru.itmo.organization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportAddressDto {

    @Size(min = 7, message = "Почтовый индекс должен содержать минимум 7 символов")
    private String zipCode;

    @NotNull(message = "Город обязателен")
    private ImportLocationDto town;
}
