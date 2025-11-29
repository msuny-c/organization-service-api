package ru.itmo.organization.dto;

import lombok.Data;

@Data
public class DeleteRequestDto {
    private Boolean cascadeDelete = false;
}
