package dev.mfikri.widuriestock.constraint;

import dev.mfikri.widuriestock.entity.Role;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(
        validatedBy = {CheckRoleValidator.class}
)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckRole {
    String message() default "Invalid role. Allowed values: ADMIN_WAREHOUSE, ADMIN_SELLER";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
