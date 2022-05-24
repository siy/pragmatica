package org.pragmatica.example.calculator.pfj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {
    @Test
    void simple_expr_parsed_mul() {
        var expr = Calculator.parse("2 3 *").unwrap();

        assertEquals(6, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed_div() {
        var expr = Calculator.parse("6 2 /").unwrap();

        assertEquals(3, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed_sub() {
        var expr = Calculator.parse("6 2 -").unwrap();

        assertEquals(4, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed1() {
        var expr = Calculator.parse("3 sqr 4 sqr + 5 sqr -").unwrap();

        assertEquals(0, expr.eval().unwrap());
    }

    @Test
    void mul_test() {
        var expr = Calculator.parse("3 5 * 2 -").unwrap();

        assertEquals(13, expr.eval().unwrap());
    }

    @Test
    void div_test() {
        var expr = Calculator.parse("3 5 * 1 + 4 /").unwrap();

        assertEquals(4, expr.eval().unwrap());
    }
}