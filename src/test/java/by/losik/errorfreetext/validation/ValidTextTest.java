package by.losik.errorfreetext.validation;

import by.losik.errorfreetext.dto.TaskDto;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты валидации текста")
class ValidTextTest {

    private ValidText.Validator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidText.Validator();
    }

    @Nested
    @DisplayName("Валидация null и пустых значений")
    class NullAndEmptyTests {

        @Test
        @DisplayName("Должен отклонять null")
        void shouldRejectNull() {
            boolean isValid = validator.isValid(null, context);

            assertThat(isValid).isFalse();
            verifyNoInteractions(context);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n", "  \t  "})
        @DisplayName("Должен отклонять пустые строки и строки из пробелов")
        void shouldRejectEmptyAndBlankStrings(String text) {
            boolean isValid = validator.isValid(text, context);

            assertThat(isValid).isFalse();
            verifyNoInteractions(context);
        }
    }

    @Nested
    @DisplayName("Валидация длины текста")
    class LengthValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "ab", "a", "12", "!@", "  a  ", "  1  ", "  !  "
        })
        @DisplayName("Должен отклонять текст длиной < 3 символов после trim")
        void shouldRejectTextShorterThan3CharsAfterTrim(String text) {
            boolean isValid = validator.isValid(text, context);

            assertThat(isValid).isFalse();
            verifyNoInteractions(context);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "abc",
                "при",
                "a1b",
                "a!b",
                "  abc  ",
                "123a",
                "!@#a",
                "a123",
                "a!@#",
                "1a2",
                "!a@",
                "café",
                "naïve",
                "Señor"
        })
        @DisplayName("Должен пропускать текст длиной >= 3 символов после обрубки с буквами")
        void shouldAcceptTextWithMinLengthAndLetters(String text) {
            boolean isValid = validator.isValid(text, context);

            assertThat(isValid)
                    .as("Текст '%s' должен быть валидным", text)
                    .isTrue();
            verifyNoInteractions(context);
        }

        @Nested
        @DisplayName("Валидация содержательности текста")
        class ContentValidationTests {

            @ParameterizedTest
            @ValueSource(strings = {
                    "abc",
                    "привет",
                    "Hello world",
                    "текст с цифрами 123",
                    "текст!!! со спецсимволами",
                    "123abc",
                    "a1b2c3",
                    "слово-с-дефисом",
                    "подъезд",
                    "какой-то текст",
                    "д'Артаньян",
                    "café",
                    "naïve",
                    "Señor"
            })
            @DisplayName("Должен пропускать текст с буквами")
            void shouldAcceptTextWithLetters(String text) {
                boolean isValid = validator.isValid(text, context);

                assertThat(isValid).isTrue();
                verifyNoInteractions(context);
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    "123",
                    "!@#",
                    "123!@#",
                    "!!!",
                    "...",
                    "---",
                    "___",
                    "₽$€",
                    "  123  ",
                    "  !@#  ",
                    "１２３",
                    "𝟭𝟮𝟯"
            })
            @DisplayName("Должен отклонять текст только из цифр и спецсимволов")
            void shouldRejectTextWithOnlyDigitsAndSpecialChars(String text) {
                boolean isValid = validator.isValid(text, context);

                assertThat(isValid).isFalse();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен отклонять текст из пробелов с цифрами")
            void shouldRejectSpacesWithDigits() {
                String text = "   123   ";

                boolean isValid = validator.isValid(text, context);

                assertThat(isValid).isFalse();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен отклонять текст только из эмодзи")
            void shouldRejectOnlyEmojis() {
                String onlyEmojis = "👋😊👍🎉";

                boolean isValid = validator.isValid(onlyEmojis, context);

                assertThat(isValid).isFalse();
                verifyNoInteractions(context);
            }
        }

        @Nested
        @DisplayName("Граничные случаи")
        class BoundaryTests {

            @Test
            @DisplayName("Должен обрабатывать очень длинный текст")
            void shouldHandleVeryLongText() {
                String longText = "a".repeat(10000);

                boolean isValid = validator.isValid(longText, context);

                assertThat(isValid).isTrue();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен обрабатывать текст с Unicode символами")
            void shouldHandleUnicodeCharacters() {
                String unicodeText = "Привет мир! こんにちは 你好";

                boolean isValid = validator.isValid(unicodeText, context);

                assertThat(isValid).isTrue();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен обрабатывать текст с эмодзи и буквами")
            void shouldHandleEmojisWithLetters() {
                String textWithEmoji = "Привет! 👋 Как дела? 😊";

                boolean isValid = validator.isValid(textWithEmoji, context);

                assertThat(isValid).isTrue();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен отклонять текст ровно из 3 цифр")
            void shouldRejectExactly3Digits() {
                String text = "123";

                boolean isValid = validator.isValid(text, context);

                assertThat(isValid).isFalse();
                verifyNoInteractions(context);
            }

            @Test
            @DisplayName("Должен пропускать текст ровно из 3 букв")
            void shouldAcceptExactly3Letters() {
                String text = "abc";

                boolean isValid = validator.isValid(text, context);

                assertThat(isValid).isTrue();
                verifyNoInteractions(context);
            }
        }

        @Nested
        @DisplayName("Тесты аннотации")
        class AnnotationTests {

            @Test
            @DisplayName("Должен иметь правильное сообщение по умолчанию")
            void shouldHaveCorrectDefaultMessage() throws NoSuchFieldException {
                ValidText annotation = TaskDto.CreateRequest.class
                        .getDeclaredField("text")
                        .getAnnotation(ValidText.class);

                String message = annotation.message();

                assertThat(message).isEqualTo("Text must contain at least 3 characters and not be only digits or special characters");
            }

            @Test
            @DisplayName("Должен быть применим к полям")
            void shouldBeTargetedToFields() {
                Target target = ValidText.class.getAnnotation(Target.class);

                ElementType[] types = target.value();

                assertThat(types).contains(ElementType.FIELD);
            }

            @Test
            @DisplayName("Должен быть доступен во время выполнения")
            void shouldBeRetainedAtRuntime() {
                Retention retention = ValidText.class.getAnnotation(Retention.class);

                assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
            }

            @Test
            @DisplayName("Должен иметь Constraint аннотацию")
            void shouldHaveConstraintAnnotation() {
                Constraint constraint = ValidText.class.getAnnotation(Constraint.class);

                assertThat(constraint).isNotNull();
                assertThat(constraint.validatedBy()).contains(ValidText.Validator.class);
            }
        }
    }
}