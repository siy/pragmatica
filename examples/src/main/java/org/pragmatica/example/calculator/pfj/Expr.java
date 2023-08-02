package org.pragmatica.example.calculator.pfj;

import org.pragmatica.lang.Result;

import static org.pragmatica.example.calculator.pfj.EvalErrors.DIVISION_BY_ZERO;
import static org.pragmatica.example.calculator.pfj.EvalErrors.INTEGER_OVERFLOW;
import static org.pragmatica.lang.Result.*;

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
            return value.eval().flatMap(Sqr::square);
        }

        static Result<Long> square(long value) {
            return Mul.mul(value, value);
        }
    }

    record Add(Expr addend, Expr augend) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(augend.eval(), addend.eval())
                .flatMap(Add::add);
        }

        public static Result<Long> add(long augend, long addend) {
            return lift(INTEGER_OVERFLOW, () -> Math.addExact(augend, addend));
        }
    }

    record Sub(Expr subtrahend, Expr minuend) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(minuend.eval(), subtrahend.eval())
                .flatMap(Sub::sub);
        }

        public static Result<Long> sub(long minuend, long subtrahend) {
            return lift(INTEGER_OVERFLOW, () -> Math.subtractExact(minuend, subtrahend));
        }
    }

    record Mul(Expr multiplicand, Expr multiplier) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(multiplier.eval(), multiplicand.eval())
                .flatMap(Mul::mul);
        }

        public static Result<Long> mul(long multiplier, long multiplicand) {
            return lift(INTEGER_OVERFLOW, () -> Math.multiplyExact(multiplier, multiplicand));
        }
    }

    record Div(Expr divisor, Expr dividend) implements Expr {
        @Override
        public Result<Long> eval() {
            return all(dividend.eval(), divisor.eval())
                .flatMap(Div::div);
        }

        public static Result<Long> div(long dividend, long divisor) {
            return divisor == 0
                   ? DIVISION_BY_ZERO.result()
                   : success(dividend / divisor);
        }
    }
}
