package org.pragmatica.example.calculator;

import org.pragmatica.lang.Result;

import java.util.StringTokenizer;

import static org.pragmatica.example.calculator.EvalErrors.DIVISION_BY_ZERO;
import static org.pragmatica.example.calculator.EvalErrors.INTEGER_OVERFLOW;
import static org.pragmatica.lang.Result.all;
import static org.pragmatica.lang.Result.success;

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
    }

    static Result<Long> square(long value) {
        return mul(value, value);
    }

    static Result<Long> add(long left, long right) {
        return Result.lift(INTEGER_OVERFLOW, () -> Math.addExact(left, right));
    }

    static Result<Long> sub(long left, long right) {
        return Result.lift(INTEGER_OVERFLOW, () -> Math.subtractExact(left, right));
    }

    static Result<Long> mul(long left, long right) {
        return Result.lift(INTEGER_OVERFLOW, () -> Math.multiplyExact(left, right));
    }

    static Result<Long> div(long left, long right) {
        return right == 0
               ? DIVISION_BY_ZERO.result()
               : success(left/right);
    }


    static Result<Expr> parse(String input) {
        StringTokenizer
    }
}
