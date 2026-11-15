package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MathEvaluatorTest {

    @Test
    fun evaluatesArithmeticAndScientificFunctions() {
        assertEquals("14", MathEvaluator.formatResult(MathEvaluator.evaluate("2 + 3 * 4")))
        assertEquals("20", MathEvaluator.formatResult(MathEvaluator.evaluate("(2 + 3) * 4")))
        assertEquals("5", MathEvaluator.formatResult(MathEvaluator.evaluate("sqrt(25)")))
        assertEquals("8", MathEvaluator.formatResult(MathEvaluator.evaluate("2^3")))
        assertEquals("0.5", MathEvaluator.formatResult(MathEvaluator.evaluate("sin(30)")))
    }
}
