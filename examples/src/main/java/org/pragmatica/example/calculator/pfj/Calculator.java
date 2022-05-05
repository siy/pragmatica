package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

public final class Calculator {
    private Calculator() {}

    public static Result<Expr> parse(String input) {
        var parser = new Parser();

        for (var token : input.split(" ")) {
            var parsingResult = parser.parse(token);

            if (parsingResult.isFailure()) {
                return parsingResult;
            }
        }

        return parser.finalizeParsing();
    }
}
