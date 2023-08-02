package org.pragmatica.example.calculator.tij;

public class ParseException extends Exception {
    public ParseException(String message, Exception cause) {
        super(message, cause);
    }

    public ParseException(String message) {
        super(message);
    }
}
