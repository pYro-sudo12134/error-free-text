package by.losik.errorfreetext.validation;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.Language;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты валидации языка")
class ValidLanguageTest {

    private ValidLanguage.Validator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidLanguage.Validator();
    }

    @Nested
    @DisplayName("Тесты валидатора")
    class ValidatorTests {

        @ParameterizedTest
        @EnumSource(value = Language.class, names = {"EN", "RU"})
        @DisplayName("Должен пропускать EN и RU")
        void shouldAcceptEnglishAndRussian(Language language) {
            boolean isValid = validator.isValid(language, context);

            assertThat(isValid).isTrue();
            verifyNoInteractions(context);
        }

        @Test
        @DisplayName("Должен отклонять null")
        void shouldRejectNull() {
            boolean isValid = validator.isValid(null, context);

            assertThat(isValid).isFalse();
            verifyNoInteractions(context);
        }

        @Test
        @DisplayName("Должен отклонять другие языки (т.к. в ENUM только EN и RU)")
        void shouldRejectOtherLanguages() {
            assertThat(Language.values()).containsExactly(Language.RU, Language.EN);
        }
    }

    @Nested
    @DisplayName("Тесты аннотации")
    class AnnotationTests {

        @Test
        @DisplayName("Должен иметь правильное сообщение по умолчанию")
        void shouldHaveCorrectDefaultMessage() throws NoSuchFieldException {
            ValidLanguage annotation = TaskDto.CreateRequest.class
                    .getDeclaredField("language")
                    .getAnnotation(ValidLanguage.class);

            String message = annotation.message();

            assertThat(message).isEqualTo("Language must be either 'EN' or 'RU'");
        }

        @Test
        @DisplayName("Должен быть применим к полям")
        void shouldBeTargetedToFields() {
            Target target = ValidLanguage.class.getAnnotation(Target.class);

            ElementType[] types = target.value();

            assertThat(types).contains(ElementType.FIELD);
        }

        @Test
        @DisplayName("Должен быть доступен во время выполнения")
        void shouldBeRetainedAtRuntime() {
            Retention retention = ValidLanguage.class.getAnnotation(Retention.class);

            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @Test
        @DisplayName("Должен иметь Constraint аннотацию")
        void shouldHaveConstraintAnnotation() {
            Constraint constraint = ValidLanguage.class.getAnnotation(Constraint.class);

            assertThat(constraint).isNotNull();
            assertThat(constraint.validatedBy()).contains(ValidLanguage.Validator.class);
        }
    }
}