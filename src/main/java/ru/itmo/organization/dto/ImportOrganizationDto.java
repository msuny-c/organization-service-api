package ru.itmo.organization.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.organization.model.OrganizationType;

@Getter
@Setter
@NoArgsConstructor
public class ImportOrganizationDto {

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @NotNull(message = "Координаты обязательны")
    private ImportCoordinatesDto coordinates;

    @Positive(message = "Годовой оборот должен быть положительным")
    private Long annualTurnover;

    @NotNull(message = "Количество сотрудников обязательно")
    @Min(value = 0, message = "Количество сотрудников не может быть отрицательным")
    private Integer employeesCount;

    @Positive(message = "Рейтинг должен быть положительным")
    private Integer rating;

    @Pattern(regexp = "^(?!\\s*$).+", message = "Полное название не может быть пустой строкой")
    private String fullName;

    @NotNull(message = "Тип организации обязателен")
    private OrganizationType type;

    @NotNull(message = "Почтовый адрес обязателен")
    private ImportAddressDto postalAddress;

    private ImportAddressDto officialAddress;
}
