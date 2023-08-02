package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result.Cause;

public enum EvalErrors implements Cause {
    INTEGER_OVERFLOW("Result is too big to fit into long"),
    DIVISION_BY_ZERO("Division by zero");

    private final String message;

    EvalErrors(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
