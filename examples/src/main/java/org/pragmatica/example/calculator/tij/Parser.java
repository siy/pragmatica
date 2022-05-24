package org.pragmatica.example.calculator.tij;

import java.util.ArrayDeque;
import java.util.Deque;

class Parser {
    private final Deque<Expr> stack = new ArrayDeque<>();

    public Expr parse(String token) throws ParseException {
        switch (token) {
            case "sqr" -> {
                var expr = new Expr.Sqr(pop());
                stack.addFirst(expr);
                return expr;
            }

            case "*" -> {
                var expr = new Expr.Mul(pop(), pop());
                stack.addFirst(expr);
                return expr;
            }

            case "/" -> {
                var expr = new Expr.Div(pop(), pop());
                stack.addFirst(expr);
                return expr;
            }

            case "+" -> {
                var expr = new Expr.Add(pop(), pop());
                stack.addFirst(expr);
                return expr;
            }

            case "-" -> {
                var expr = new Expr.Sub(pop(), pop());
                stack.addFirst(expr);
                return expr;
            }

            default -> {
                var expr = new Expr.Number(parseLong(token));
                stack.addFirst(expr);
                return expr;
            }
        }
    }

    private long parseLong(String token) throws ParseException {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid value, numeric value is expected", e);
        }
    }

    public Expr finalizeParsing() throws ParseException {
        if (stack.size() != 1) {
            throw new ParseException("Input expression is invalid");
        }

        return pop();
    }

    private Expr pop() throws ParseException {
        var result = stack.pollFirst();

        if (result == null) {
            throw new ParseException("Parsing stack is empty, input expression is invalid");
        }

        return result;
    }
}
