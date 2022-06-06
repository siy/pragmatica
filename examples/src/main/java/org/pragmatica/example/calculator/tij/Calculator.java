package org.pragmatica.example.calculator.tij;

import java.util.Scanner;

public final class Calculator {
    private Calculator() {}

    public static Expr parse(String input) throws ParseException {
        var parser = new Parser();
        var scanner = new Scanner(input);

        while (scanner.hasNext()) {
            parser.parse(scanner.next());
        }

        return parser.finalizeParsing();
    }
}
