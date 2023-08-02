package org.pragmatica.example.calculator.tij;

public class EvalException extends Exception {
    public EvalException(String message, Exception cause) {
        super(message, cause);
    }

    public EvalException(String message) {
        super(message);
    }
}
