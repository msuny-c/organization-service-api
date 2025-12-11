package ru.itmo.organization.service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.repository.OrganizationRepository;
import ru.itmo.organization.validation.UniqueOrganization;

@Component
@RequiredArgsConstructor
public class UniqueConstraintValidator implements ConstraintValidator<UniqueOrganization, OrganizationDto> {

    private final OrganizationRepository organizationRepository;

    @Override
    public boolean isValid(OrganizationDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        boolean valid = true;
        Long excludeId = dto.getId();
        context.disableDefaultConstraintViolation();

        if (dto.getFullName() != null && organizationRepository.existsByFullName(dto.getFullName(), excludeId)) {
            context.buildConstraintViolationWithTemplate("Организация с таким полным названием уже существует")
                    .addPropertyNode("fullName")
                    .addConstraintViolation();
            valid = false;
        }

        if (dto.getCoordinates() != null
                && organizationRepository.existsByCoordinates(dto.getCoordinates().getX(), dto.getCoordinates().getY(), excludeId)) {
            context.buildConstraintViolationWithTemplate("Организация с такими координатами уже существует")
                    .addPropertyNode("coordinates")
                    .addConstraintViolation();
            valid = false;
        }

        if (dto.getPostalAddress() != null
                && organizationRepository.existsByPostalZip(dto.getPostalAddress().getZipCode(), excludeId)) {
            context.buildConstraintViolationWithTemplate("Почтовый индекс почтового адреса должен быть уникальным")
                    .addPropertyNode("postalAddress.zipCode")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
