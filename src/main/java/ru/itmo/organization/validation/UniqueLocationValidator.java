package ru.itmo.organization.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.organization.dto.LocationDto;
import ru.itmo.organization.repository.LocationRepository;

@Component
@RequiredArgsConstructor
public class UniqueLocationValidator implements ConstraintValidator<UniqueLocation, LocationDto> {

    private final LocationRepository locationRepository;

    @Override
    public boolean isValid(LocationDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        Long excludeId = dto.getId();
        boolean exists = dto.getName() != null && locationRepository.existsByName(dto.getName(), excludeId);
        if (exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Локация с таким названием уже существует")
                    .addPropertyNode("name")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
