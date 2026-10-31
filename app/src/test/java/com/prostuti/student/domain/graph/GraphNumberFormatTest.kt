package com.prostuti.student.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class GraphNumberFormatTest {

    @Test fun formatsWithoutFloatNoise() {
        assertEquals("2.66667", GraphNumberFormat.format(8.0 / 3))
        assertEquals("0.3", GraphNumberFormat.format(0.1 + 0.2))
        assertEquals("0", GraphNumberFormat.format(1e-17))
        assertEquals("−2.5", GraphNumberFormat.format(-2.5))
        assertEquals("1.23457×10⁹", GraphNumberFormat.format(1234567890.0))
        assertEquals("1.5×10⁻⁷", GraphNumberFormat.format(1.5e-7))
        assertEquals("undefined", GraphNumberFormat.format(Double.NaN))
    }

    @Test fun findsExactForms() {
        assertEquals("8/3", GraphNumberFormat.exactForm(8.0 / 3))
        assertEquals("−1/6", GraphNumberFormat.exactForm(-1.0 / 6))
        assertEquals("π/2", GraphNumberFormat.exactForm(PI / 2))
        assertEquals("π", GraphNumberFormat.exactForm(PI))
        assertEquals("3π/4", GraphNumberFormat.exactForm(3 * PI / 4))
        assertEquals("√2", GraphNumberFormat.exactForm(sqrt(2.0)))
        assertEquals("√3/2", GraphNumberFormat.exactForm(sqrt(3.0) / 2))
        assertNull(GraphNumberFormat.exactForm(2.0)) // integers need no extra form
        assertNull(GraphNumberFormat.exactForm(kotlin.math.E))
    }
}
