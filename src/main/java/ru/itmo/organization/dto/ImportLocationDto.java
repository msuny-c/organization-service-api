package ru.itmo.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportLocationDto {

    @NotNull(message = "Координата X не может быть null")
    private Long x;

    @NotNull(message = "Координата Y не может быть null")
    private Long y;

    @NotNull(message = "Координата Z не может быть null")
    private Double z;

    @NotBlank(message = "Имя не может быть пустым")
    private String name;
}
