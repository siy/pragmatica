package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

public enum EvalErrors implements Result.Cause {
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
