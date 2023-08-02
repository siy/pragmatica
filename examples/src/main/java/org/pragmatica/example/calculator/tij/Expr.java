package org.pragmatica.example.calculator.tij;

public sealed interface Expr {
    long eval() throws EvalException;

    record Number(long value) implements Expr {
        @Override
        public long eval() {
            return value;
        }
    }

    record Sqr(Expr value) implements Expr {
        @Override
        public long eval() throws EvalException {
            return square(value.eval());
        }

        static long square(long value) throws EvalException {
            return Mul.mul(value, value);
        }
    }

    record Add(Expr addend, Expr augend) implements Expr {
        @Override
        public long eval() throws EvalException {
            return add(augend.eval(), addend.eval());
        }

        public static long add(long augend, long addend) throws EvalException {
            try {
                return Math.addExact(augend, addend);
            } catch (ArithmeticException e) {
                throw new EvalException("Integer overflow", e);
            }
        }
    }

    record Sub(Expr subtrahend, Expr minuend) implements Expr {
        @Override
        public long eval() throws EvalException {
            return sub(minuend.eval(), subtrahend.eval());
        }

        public static long sub(long minuend, long subtrahend) throws EvalException {
            try {
                return Math.subtractExact(minuend, subtrahend);
            } catch (ArithmeticException e) {
                throw new EvalException("Integer overflow", e);
            }
        }
    }

    record Mul(Expr multiplicand, Expr multiplier) implements Expr {
        @Override
        public long eval() throws EvalException {
            return mul(multiplier.eval(), multiplicand.eval());
        }

        public static long mul(long multiplier, long multiplicand) throws EvalException {
            try {
                return Math.multiplyExact(multiplier, multiplicand);
            } catch (ArithmeticException e) {
                throw new EvalException("Integer overflow", e);
            }
        }
    }

    record Div(Expr divisor, Expr dividend) implements Expr {
        @Override
        public long eval() throws EvalException {
            return div(dividend.eval(), divisor.eval());
        }

        public static long div(long dividend, long divisor) throws EvalException {
            if (divisor == 0) {
                throw new EvalException("Division by zero");
            }

            return dividend / divisor;
        }
    }
}
