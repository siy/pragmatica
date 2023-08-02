package org.pragmatica.example.calculator.pfj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExprTest {
    @Test
    void simpleExpressionIsEvaluatedProperly() {
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
        var expr = new Expr.Sub(new Expr.Number(2), new Expr.Number(3));

        assertEquals(1, expr.eval().unwrap());
    }

    @Test
    void simple_expr3() {
        var expr = new Expr.Mul(new Expr.Number(3), new Expr.Number(2));

        assertEquals(6, expr.eval().unwrap());
    }

    @Test
    void simple_expr4() {
        var expr = new Expr.Div(new Expr.Number(3), new Expr.Number(6));

        assertEquals(2, expr.eval().unwrap());
    }

    @Test
    void simple_expr5() {
        var expr = new Expr.Sqr(new Expr.Number(6));

        assertEquals(36, expr.eval().unwrap());
    }

    @Test
    void cant_div_by_zero() {
        var expr = new Expr.Div(new Expr.Number(0), new Expr.Number(1));

        expr.eval().onSuccessDo(Assertions::fail);
    }
}