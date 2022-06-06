package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

import java.util.Scanner;

public final class Calculator {
    private Calculator() {}

    public static Result<Expr> parse(String input) {
        var parser = new Parser();

        return new Scanner(input).tokens()
            .map(parser::parse)
            .filter(Result::isFailure)
            .findAny()
            .orElseGet(parser::finalizeParsing);
    }
}
