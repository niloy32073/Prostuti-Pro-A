package com.prostuti.student.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcNumberFormatterTest {

    private val formatter = CalcNumberFormatter()
    private val norm1 = CalcNumberFormat.Norm(1)

    @Test fun `plain numbers keep up to 10 digits`() {
        assertEquals(FormattedNumber("3.333333333"), formatter.format(10.0 / 3, norm1))
        assertEquals(FormattedNumber("1234"), formatter.format(1234.0, norm1))
    }

    @Test fun `norm 1 switches to scientific below 0_01`() {
        assertEquals(FormattedNumber("5", -3), formatter.format(0.005, norm1))
        assertEquals(FormattedNumber("0.005"), formatter.format(0.005, CalcNumberFormat.Norm(2)))
    }

    @Test fun `large values use scientific notation`() =
        assertEquals(FormattedNumber("1.23456789", 12), formatter.format(1.23456789e12, norm1))

    @Test fun `fix and sci`() {
        assertEquals(FormattedNumber("3.14"), formatter.format(Math.PI, CalcNumberFormat.Fix(2)))
        assertEquals(FormattedNumber("3.142", 0), formatter.format(Math.PI, CalcNumberFormat.Sci(4)))
    }

    @Test fun `engineering steps`() {
        assertEquals(FormattedNumber("1.234", 3), formatter.formatEngineering(1234.0, 0))
        assertEquals(FormattedNumber("1234", 0), formatter.formatEngineering(1234.0, -1))
    }

    @Test fun `fractions for rational results only`() {
        assertEquals(CalcFraction(false, 1, 3), formatter.fraction(CalcMath.roundSignificant(1.0 / 3, 15)))
        assertEquals(CalcFraction(true, 7, 4), formatter.fraction(-1.75))
        assertNull(formatter.fraction(Math.PI))
        assertNull(formatter.fraction(Math.sqrt(2.0)))
        assertNull(formatter.fraction(5.0))
    }

    @Test fun `degrees minutes seconds`() = assertEquals("12°30°0°", formatter.sexagesimal(12.5))
}
