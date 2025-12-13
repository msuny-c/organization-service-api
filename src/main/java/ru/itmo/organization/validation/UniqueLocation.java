package ru.itmo.organization.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE_USE, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ru.itmo.organization.validation.UniqueLocationValidator.class)
public @interface UniqueLocation {
    String message() default "Локация с таким названием уже существует";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
