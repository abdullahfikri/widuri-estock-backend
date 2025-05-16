package dev.mfikri.widuriestock.constraint;

import dev.mfikri.widuriestock.entity.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckRoleValidator implements ConstraintValidator<CheckRole, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true; // null handled by @NotNull
        }

        try {
            Role.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }

    }
}
