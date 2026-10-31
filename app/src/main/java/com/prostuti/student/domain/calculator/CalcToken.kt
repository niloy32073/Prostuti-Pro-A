package com.prostuti.student.domain.calculator

/**
 * Token ids stored by the calculator editor. One key press inserts one token, so DEL removes a
 * whole "sin(" at once, like the real calculator. Digits "0".."9" and "." are their own ids.
 */
object CalcToken {
    const val DOT = "."
    const val EXP10 = "exp10"

    const val PLUS = "+"
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"
    const val FRACTION = "frac"
    const val NPR = "nPr"
    const val NCR = "nCr"
    const val POWER = "pow("
    const val XROOT = "xroot("

    const val SQUARE = "²"
    const val CUBE = "³"
    const val INVERSE = "⁻¹"
    const val FACTORIAL = "!"
    const val PERCENT = "%"
    const val DMS = "dms"
    const val UNIT_DEG = "unit_deg"
    const val UNIT_RAD = "unit_rad"
    const val UNIT_GRAD = "unit_grad"

    const val NEG = "neg"
    const val OPEN = "("
    const val CLOSE = ")"
    const val COMMA = ","
    const val EQUALS = "eq"

    const val SIN = "sin("
    const val COS = "cos("
    const val TAN = "tan("
    const val ASIN = "asin("
    const val ACOS = "acos("
    const val ATAN = "atan("
    const val SINH = "sinh("
    const val COSH = "cosh("
    const val TANH = "tanh("
    const val ASINH = "asinh("
    const val ACOSH = "acosh("
    const val ATANH = "atanh("
    const val LOG = "log("
    const val LN = "ln("
    const val POW10 = "pow10("
    const val EXP = "exp("
    const val SQRT = "sqrt("
    const val CBRT = "cbrt("
    const val ABS = "abs("
    const val RND = "rnd("
    const val INTEGRAL = "integral("
    const val DERIVATIVE = "derivative("
    const val SUM = "sum("

    const val PI = "pi"
    const val E = "const_e"
    const val ANS = "ans"
    const val RANDOM = "ran"

    private const val VAR_PREFIX = "var_"

    /** Variable names in the order the calculator lists them. */
    val VARIABLES = listOf("A", "B", "C", "D", "E", "F", "X", "Y", "M")

    fun variable(name: String): String = VAR_PREFIX + name
    fun variableName(token: String): String? =
        if (token.startsWith(VAR_PREFIX)) token.removePrefix(VAR_PREFIX) else null

    fun isDigit(token: String): Boolean = token.length == 1 && token[0] in '0'..'9'

    /** Functions whose argument list starts with the token itself (the token includes the "("). */
    val FUNCTIONS = setOf(
        SIN, COS, TAN, ASIN, ACOS, ATAN, SINH, COSH, TANH, ASINH, ACOSH, ATANH,
        LOG, LN, POW10, EXP, SQRT, CBRT, ABS, RND, INTEGRAL, DERIVATIVE, SUM,
    )

    /** Tokens that continue the previous answer when typed right after a result ("Ans+…"). */
    val CONTINUES_ANSWER = setOf(
        PLUS, MINUS, TIMES, DIVIDE, FRACTION, NPR, NCR, POWER, XROOT,
        SQUARE, CUBE, INVERSE, FACTORIAL, PERCENT, DMS, UNIT_DEG, UNIT_RAD, UNIT_GRAD,
    )

    private val DISPLAY = mapOf(
        EXP10 to "ᴇ", FRACTION to "⌟", NPR to "P", NCR to "C", POWER to "^(", XROOT to "ˣ√(",
        DMS to "°", UNIT_DEG to "°", UNIT_RAD to "ʳ", UNIT_GRAD to "ᵍ", NEG to "-", EQUALS to "=",
        ASIN to "sin⁻¹(", ACOS to "cos⁻¹(", ATAN to "tan⁻¹(",
        ASINH to "sinh⁻¹(", ACOSH to "cosh⁻¹(", ATANH to "tanh⁻¹(",
        POW10 to "10^(", EXP to "e^(", SQRT to "√(", CBRT to "∛(", ABS to "Abs(", RND to "Rnd(",
        INTEGRAL to "∫(", DERIVATIVE to "d/dx(", SUM to "Σ(",
        PI to "π", E to "e", ANS to "Ans", RANDOM to "Ran#",
    )

    fun display(token: String): String = variableName(token) ?: DISPLAY[token] ?: token

    fun display(tokens: List<String>): String = tokens.joinToString("") { display(it) }
}
