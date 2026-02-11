package by.losik.errorfreetext.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class EnumUtils {

    public static String formatEnumValues(Class<?> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum: " + enumClass);
        }

        return Arrays.stream(enumClass.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    public static String getEnumValuesSafe(Class<?> enumClass) {
        try {
            return formatEnumValues(enumClass);
        } catch (Exception e) {
            log.warn("Failed to get enum values for class: {}", enumClass.getSimpleName(), e);
            return "???";
        }
    }
}