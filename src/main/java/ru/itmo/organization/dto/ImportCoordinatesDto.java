package ru.itmo.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportCoordinatesDto {

    @NotNull(message = "Координата X не может быть null")
    private Long x;

    @NotNull(message = "Координата Y не может быть null")
    private Long y;
}
