package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Result.all;

class Parser {
    private final Deque<Expr> stack = new ArrayDeque<>();

    public Result<Expr> parse(String token) {
        return switch (token) {
            case "sqr" -> pop()
                .map(Expr.Sqr::new)
                .map(this::push);

            case "*" -> all(pop(), pop())
                .map(Expr.Mul::new)
                .map(this::push);

            case "/" -> all(pop(), pop())
                .map(Expr.Div::new)
                .map(this::push);

            case "+" -> all(pop(), pop())
                .map(Expr.Add::new)
                .map(this::push);

            case "-" -> all(pop(), pop())
                .map(Expr.Sub::new)
                .map(this::push);

            default -> parseLong(token)
                .map(Expr.Number::new)
                .map(this::push);
        };
    }

    private Result<Long> parseLong(String token) {
        return Result.lift(ParseErrors.INVALID_NUMBER, () -> Long.parseLong(token));
    }

    public Result<Expr> finalizeParsing() {
        if (stack.size() != 1) {
            return ParseErrors.INVALID_EXPRESSION.result();
        }

        return pop();
    }

    private Result<Expr> pop() {
        return option(stack.pollFirst())
            .toResult(ParseErrors.PARSING_STACK_IS_EMPTY);
    }

    public Expr push(Expr expr) {
        stack.addFirst(expr);
        return expr;
    }
}
