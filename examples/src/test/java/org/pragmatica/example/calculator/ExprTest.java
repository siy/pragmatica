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
}