package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Cause;

public enum ParseErrors implements Cause {
    INVALID_EXPRESSION("Input expression is invalid"),
    INVALID_NUMBER("Invalid value, numeric value is expected"),
    PARSING_STACK_IS_EMPTY("Parsing stack is empty, input expression is invalid");

    private String message;

    ParseErrors(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
