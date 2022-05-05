package org.pragmatica.example.calculator;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.Tuple.Tuple2;

import static org.pragmatica.example.calculator.EvalErrors.DIVISION_BY_ZERO;
import static org.pragmatica.example.calculator.EvalErrors.INTEGER_OVERFLOW;
import static org.pragmatica.example.calculator.ParseErrors.*;
import static org.pragmatica.lang.Result.*;
import static org.pragmatica.lang.Tuple.tuple;

public sealed interface Expr {
    Result<Long> eval();

    record Number(long value) implements Expr {
        @Override
        public Result<Long> eval() {
            return success(value);
        }
    }

    record Sqr(Expr value) implements Expr {
        @Override
        public Result<Long> eval() {
            return value.eval().flatMap(Expr::square);
        }
    }

    record Add(Expr left, Expr right) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(left.eval(), right.eval())
                .flatMap(Expr::add);
        }
    }

    record Sub(Expr left, Expr right) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(left.eval(), right.eval())
                .flatMap(Expr::sub);
        }

        static Sub swap(Expr left, Expr right) {
            return new Sub(right, left);
        }
    }

    record Mul(Expr left, Expr right) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(left.eval(), right.eval())
                .flatMap(Expr::mul);
        }
    }

    record Div(Expr left, Expr right) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(left.eval(), right.eval())
                .flatMap(Expr::div);
        }

        static Div swap(Expr left, Expr right) {
            return new Div(right, left);
        }
    }

    static Result<Long> square(long value) {
        return mul(value, value);
    }

    static Result<Long> add(long left, long right) {
        return lift(INTEGER_OVERFLOW, () -> Math.addExact(left, right));
    }

    static Result<Long> sub(long left, long right) {
        return lift(INTEGER_OVERFLOW, () -> Math.subtractExact(left, right));
    }

    static Result<Long> mul(long left, long right) {
        return lift(INTEGER_OVERFLOW, () -> Math.multiplyExact(left, right));
    }

    static Result<Long> div(long left, long right) {
        return right == 0
               ? DIVISION_BY_ZERO.result()
               : success(left / right);
    }

    class Parser {
        private final Stack<Expr> stack = new Stack<>();

        Result<Expr> parse(String token) {
            return switch (token) {
                case "sqr" -> pop().map(Sqr::new).onSuccess(stack::push).map(Expr.class::cast);
                case "*" -> all(pop(), pop()).map(Mul::new).onSuccess(stack::push).map(Expr.class::cast);
                case "/" -> all(pop(), pop()).map(Div::swap).onSuccess(stack::push).map(Expr.class::cast);
                case "+" -> all(pop(), pop()).map(Add::new).onSuccess(stack::push).map(Expr.class::cast);
                case "-" -> all(pop(), pop()).map(Sub::swap).onSuccess(stack::push).map(Expr.class::cast);
                default -> parseLong(token).map(Number::new).onSuccess(stack::push).map(Expr.class::cast);
            };
        }

        private Result<Long> parseLong(String token) {
            return lift(INVALID_NUMBER, () -> Long.parseLong(token));
        }

        public Result<Expr> finalizeParsing() {
            if (stack.size() != 1) {
                return INVALID_EXPRESSION.result();
            }

            return pop();
        }

        private Result<Expr> pop() {
            return stack.pop().toResult(PARSING_STACK_IS_EMPTY);
        }
    }

    static Result<Expr> parse(String input) {
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
