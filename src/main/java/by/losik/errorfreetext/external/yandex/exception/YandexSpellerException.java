package by.losik.errorfreetext.external.yandex.exception;

public class YandexSpellerException extends RuntimeException {

    public YandexSpellerException(String message) {
        super(message);
    }

    public YandexSpellerException(String message, Throwable cause) {
        super(message, cause);
    }
}