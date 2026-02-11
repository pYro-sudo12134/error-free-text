package by.losik.errorfreetext.validation;

import by.losik.errorfreetext.entity.Language;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidLanguage.Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLanguage {

    String message() default "Language must be either 'EN' or 'RU'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidLanguage, Language> {

        @Override
        public boolean isValid(Language language, ConstraintValidatorContext context) {
            if (language == null) {
                return false;
            }

            // Проверяем, что язык один из разрешенных
            return language == Language.EN || language == Language.RU;
        }
    }
}