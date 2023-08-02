package org.pragmatica.example.calculator.tij;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExprTest {
    @Test
    void simpleExpressionIsEvaluatedProperly() throws Exception {
        var expr = new Expr.Div(
            new Expr.Number(3),
            new Expr.Mul(
                new Expr.Number(2L),
                new Expr.Add(
                    new Expr.Number(1),
                    new Expr.Sub(
                        new Expr.Number(2),
                        new Expr.Number(4))))
        );

        assertEquals(2, expr.eval());
    }

    @Test
    void it_works() {
        var expr = new Expr.Number(10);

        assertEquals(10, expr.eval());
    }

    @Test
    void simple_expr1() throws Exception {
        var expr = new Expr.Add(new Expr.Number(1), new Expr.Number(2));

        assertEquals(3, expr.eval());
    }

    @Test
    void simple_expr2() throws Exception {
        var expr = new Expr.Sub(new Expr.Number(2), new Expr.Number(3));

        assertEquals(1, expr.eval());
    }

    @Test
    void simple_expr3() throws Exception {
        var expr = new Expr.Mul(new Expr.Number(3), new Expr.Number(2));

        assertEquals(6, expr.eval());
    }

    @Test
    void simple_expr4() throws Exception {
        var expr = new Expr.Div(new Expr.Number(3), new Expr.Number(6));

        assertEquals(2, expr.eval());
    }

    @Test
    void simple_expr5() throws Exception {
        var expr = new Expr.Sqr(new Expr.Number(6));

        assertEquals(36, expr.eval());
    }

    @Test
    void cant_div_by_zero() {
        var expr = new Expr.Div(new Expr.Number(0), new Expr.Number(1));

        assertThrows(EvalException.class, expr::eval);
    }
}