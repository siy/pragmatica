package org.pragmatica.example.calculator.tij;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {
    @Test
    void simple_expr_parsed_mul() throws Exception {
        var expr = Calculator.parse("2 3 *");

        assertEquals(6, expr.eval());
    }

    @Test
    void simple_expr_parsed_div() throws Exception {
        var expr = Calculator.parse("6 2 /");

        assertEquals(3, expr.eval());
    }

    @Test
    void simple_expr_parsed_sub() throws Exception {
        var expr = Calculator.parse("6 2 -");

        assertEquals(4, expr.eval());
    }

    @Test
    void simple_expr_parsed1() throws Exception {
        var expr = Calculator.parse("3 sqr 4 sqr + 5 sqr -");

        assertEquals(0, expr.eval());
    }

    @Test
    void mul_test() throws Exception {
        var expr = Calculator.parse("3 5 * 2 -");

        assertEquals(13, expr.eval());
    }

    @Test
    void div_test() throws Exception {
        var expr = Calculator.parse("3 5 * 1 + 4 /");

        assertEquals(4, expr.eval());
    }
}