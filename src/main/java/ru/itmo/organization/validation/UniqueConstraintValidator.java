package ru.itmo.organization.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.OrganizationDto;
import ru.itmo.organization.repository.OrganizationRepository;

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

        return valid;
    }
}
