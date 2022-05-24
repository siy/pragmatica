package org.pragmatica.example.calculator.tij;

public final class Calculator {
    private Calculator() {}

    public static Expr parse(String input) throws ParseException {
        var parser = new Parser();

        for (var token : input.split(" ")) {
            parser.parse(token);
        }

        return parser.finalizeParsing();
    }
}
