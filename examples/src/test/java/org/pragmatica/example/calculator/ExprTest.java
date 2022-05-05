package org.pragmatica.example.calculator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExprTest {
    @Test
    void simpleExpressionIsEvaluatedProperly() {
        var expr = new Expr.Div(
            new Expr.Mul(
                new Expr.Number(2L),
                new Expr.Add(
                    new Expr.Number(1),
                    new Expr.Sub(
                        new Expr.Number(4),
                        new Expr.Number(2)))),
            new Expr.Number(3)
        );

        expr.eval()
            .onFailureDo(Assertions::fail)
            .onSuccess(value -> assertEquals(2, value));
    }

    @Test
    void it_works() {
        var expr = new Expr.Number(10);

        assertEquals(10, expr.eval().unwrap());
    }

    @Test
    void simple_expr1() {
        var expr = new Expr.Add(new Expr.Number(1), new Expr.Number(2));

        assertEquals(3, expr.eval().unwrap());
    }

    @Test
    void simple_expr2() {
        var expr = new Expr.Sub(new Expr.Number(3), new Expr.Number(2));

        assertEquals(1, expr.eval().unwrap());
    }

    @Test
    void simple_expr3() {
        var expr = new Expr.Mul(new Expr.Number(3), new Expr.Number(2));

        assertEquals(6, expr.eval().unwrap());
    }

    @Test
    void simple_expr4() {
        var expr = new Expr.Div(new Expr.Number(6), new Expr.Number(3));

        assertEquals(2, expr.eval().unwrap());
    }

    @Test
    void simple_expr5() {
        var expr = new Expr.Sqr(new Expr.Number(6));

        assertEquals(36, expr.eval().unwrap());
    }

    @Test
    void cant_div_by_zero() {
        var expr = new Expr.Div(new Expr.Number(1), new Expr.Number(0));

        expr.eval().onSuccessDo(Assertions::fail);
    }

    @Test
    void simple_expr_parsed_mul() {
        var expr = Expr.parse("2 3 *").unwrap();

        assertEquals(6, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed_div() {
        var expr = Expr.parse("6 2 /").unwrap();

        assertEquals(3, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed_sub() {
        var expr = Expr.parse("6 2 -").unwrap();

        assertEquals(4, expr.eval().unwrap());
    }

    @Test
    void simple_expr_parsed1() {
        var expr = Expr.parse("3 sqr 4 sqr + 5 sqr -").unwrap();

        assertEquals(0, expr.eval().unwrap());
    }

    @Test
    void mul_test() {
        var expr = Expr.parse("3 5 * 2 -").unwrap();

        assertEquals(13, expr.eval().unwrap());
    }

    @Test
    void div_test() {
        var expr = Expr.parse("3 5 * 1 + 4 /").unwrap();

        assertEquals(4, expr.eval().unwrap());
    }
}