package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

import static org.pragmatica.lang.Result.all;

class Parser {
    private final Stack<Expr> stack = new Stack<>();

    Result<Expr> parse(String token) {
        return switch (token) {
            case "sqr" -> pop().map(Expr.Sqr::new).onSuccess(stack::push).map(Expr.class::cast);
            case "*" -> all(pop(), pop()).map(Expr.Mul::new).onSuccess(stack::push).map(Expr.class::cast);
            case "/" -> all(pop(), pop()).map(Expr.Div::new).onSuccess(stack::push).map(Expr.class::cast);
            case "+" -> all(pop(), pop()).map(Expr.Add::new).onSuccess(stack::push).map(Expr.class::cast);
            case "-" -> all(pop(), pop()).map(Expr.Sub::new).onSuccess(stack::push).map(Expr.class::cast);
            default -> parseLong(token).map(Expr.Number::new).onSuccess(stack::push).map(Expr.class::cast);
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
        return stack.pop().toResult(ParseErrors.PARSING_STACK_IS_EMPTY);
    }
}
