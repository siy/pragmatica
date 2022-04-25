package org.pragmatica.example.calculator;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

public enum EvalErrors implements Cause {
    INTEGER_OVERFLOW("Result is too big to fit into long"),
    DIVISION_BY_ZERO("Division by zero");

    private String message;

    EvalErrors(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
