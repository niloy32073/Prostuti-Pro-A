package com.prostuti.student.domain.graph

/** How a built-in function uses angles: trig reads an angle, inverse trig returns one. */
internal enum class AngleRole { NONE, INPUT, OUTPUT }

internal data class BuiltinFunction(
    val name: String,
    val minArgs: Int,
    val maxArgs: Int = minArgs,
    val angle: AngleRole = AngleRole.NONE,
    /** Name of the inverse for the `sin^-1` / `sin⁻¹` spelling. */
    val inverse: String? = null,
    /** Describes a whole line (an arrow, a solid) rather than a number; invalid inside expressions. */
    val structural: Boolean = false,
)

/** The built-in functions, their spellings, and the multi-letter words the tokenizer knows. */
internal object MathFunctions {

    private val list = listOf(
        BuiltinFunction("sin", 1, angle = AngleRole.INPUT, inverse = "asin"),
        BuiltinFunction("cos", 1, angle = AngleRole.INPUT, inverse = "acos"),
        BuiltinFunction("tan", 1, angle = AngleRole.INPUT, inverse = "atan"),
        BuiltinFunction("sec", 1, angle = AngleRole.INPUT, inverse = "asec"),
        BuiltinFunction("csc", 1, angle = AngleRole.INPUT, inverse = "acsc"),
        BuiltinFunction("cot", 1, angle = AngleRole.INPUT, inverse = "acot"),
        BuiltinFunction("asin", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("acos", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("atan", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("asec", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("acsc", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("acot", 1, angle = AngleRole.OUTPUT),
        BuiltinFunction("sinh", 1, inverse = "asinh"),
        BuiltinFunction("cosh", 1, inverse = "acosh"),
        BuiltinFunction("tanh", 1, inverse = "atanh"),
        BuiltinFunction("sech", 1),
        BuiltinFunction("csch", 1),
        BuiltinFunction("coth", 1),
        BuiltinFunction("asinh", 1),
        BuiltinFunction("acosh", 1),
        BuiltinFunction("atanh", 1),
        BuiltinFunction("ln", 1),
        // log(x) is base 10; log_b(x) is parsed as log(x, b).
        BuiltinFunction("log", 1, 2),
        BuiltinFunction("exp", 1),
        BuiltinFunction("sqrt", 1),
        BuiltinFunction("cbrt", 1),
        BuiltinFunction("nthroot", 2),
        BuiltinFunction("abs", 1),
        BuiltinFunction("sign", 1),
        BuiltinFunction("floor", 1),
        BuiltinFunction("ceil", 1),
        BuiltinFunction("round", 1),
        BuiltinFunction("mod", 2),
        BuiltinFunction("min", 1, 20),
        BuiltinFunction("max", 1, 20),
        BuiltinFunction("gcd", 2),
        BuiltinFunction("lcm", 2),
        BuiltinFunction("nCr", 2),
        BuiltinFunction("nPr", 2),
        BuiltinFunction("factorial", 1),
        BuiltinFunction("gamma", 1),
        // Whole-line forms, not values: vector((0,0,0), (1,2,3)), revolvex(f, a, b), revolvey(f, g, a, b).
        BuiltinFunction("vector", 1, 2, structural = true),
        BuiltinFunction("field", 2, 2, structural = true),
        BuiltinFunction("revolvex", 3, 4, structural = true),
        BuiltinFunction("revolvey", 3, 4, structural = true),
    )

    private val byName: Map<String, BuiltinFunction> = list.associateBy { it.name }

    private val aliases = mapOf(
        "arcsin" to "asin", "arccos" to "acos", "arctan" to "atan",
        "arcsec" to "asec", "arccsc" to "acsc", "arccot" to "acot",
        "cosec" to "csc",
        "arsinh" to "asinh", "arcosh" to "acosh", "artanh" to "atanh",
        "arcsinh" to "asinh", "arccosh" to "acosh", "arctanh" to "atanh",
        "sgn" to "sign", "root" to "nthroot",
    )

    /** Constants and Greek letters typed as words. */
    val wordSymbols = mapOf(
        "pi" to "π", "tau" to "τ", "theta" to "θ", "phi" to "φ",
        // "gamma" is the Γ function, so γ is only typed directly.
        "alpha" to "α", "beta" to "β", "omega" to "ω", "lambda" to "λ",
        "infinity" to "∞", "infty" to "∞", "inf" to "∞",
    )

    /** Every multi-letter word, longest first, so "sinh" wins over "sin" and "sign" over "sin"+"g". */
    val words: List<String> =
        (byName.keys + aliases.keys + wordSymbols.keys).filter { it.length > 1 }.sortedByDescending { it.length }

    fun lookup(name: String): BuiltinFunction? = byName[aliases[name] ?: name]

    fun canonical(name: String): String = aliases[name] ?: name

    fun isBuiltin(name: String): Boolean = lookup(name) != null
}
