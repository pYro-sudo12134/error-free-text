package by.losik.errorfreetext.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidText.Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidText {

    String message() default "Text must contain at least 3 characters and not be only digits or special characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidText, String> {

        @Override
        public boolean isValid(String text, ConstraintValidatorContext context) {
            if (text == null) {
                return false;
            }

            String trimmed = text.trim();

            if (trimmed.length() < 3) {
                return false;
            }

            boolean hasLetter = false;
            for (char c : trimmed.toCharArray()) {
                if (Character.isLetter(c)) {
                    hasLetter = true;
                    break;
                }
            }

            return hasLetter;
        }
    }
}