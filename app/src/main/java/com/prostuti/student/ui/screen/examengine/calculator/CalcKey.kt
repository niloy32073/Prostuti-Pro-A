package com.prostuti.student.ui.screen.examengine.calculator

/**
 * The physical keys, top-left to bottom-right. [shiftLabel] / [alphaLabel] are printed above the
 * key; only functions the calculator actually performs get a label.
 */
enum class CalcKey(val label: String, val shiftLabel: String = "", val alphaLabel: String = "") {
    SHIFT("SHIFT"), ALPHA("ALPHA"), LEFT("◀"), RIGHT("▶"), UP("▲"), DOWN("▼"), MODE("MODE", "SETUP"), ON("ON"),
    CALC("CALC", "SOLVE", "="), INTEGRAL("∫dx", "d/dx"), INVERSE("x⁻¹", "x!"), LOG_AB("logₐb", "Σ"),
    FRACTION("a⁄b"), SQRT("√", "∛"), SQUARE("x²", "x³"), POWER("xʸ", "ˣ√"), LOG("log", "10ˣ"), LN("ln", "eˣ"),
    NEGATIVE("(−)", alphaLabel = "A"), DMS("°'\"", alphaLabel = "B"), HYP("hyp", "Abs", "C"),
    SIN("sin", "sin⁻¹", "D"), COS("cos", "cos⁻¹", "E"), TAN("tan", "tan⁻¹", "F"),
    RCL("RCL", "STO"), ENG("ENG", "←"), OPEN("(", "%"), CLOSE(")", ",", "X"), S_D("S⇔D", alphaLabel = "Y"),
    M_PLUS("M+", "M−", "M"),
    D7("7"), D8("8"), D9("9", "CLR"), DEL("DEL"), AC("AC"),
    D4("4"), D5("5"), D6("6"), MULTIPLY("×", "nPr"), DIVIDE("÷", "nCr"),
    D1("1"), D2("2"), D3("3"), PLUS("+"), MINUS("−"),
    D0("0", "Rnd"), DOT(".", "Ran#"), EXP10("×10ˣ", "π", "e"), ANS("Ans", "DRG▶"), EQUALS("=");

    /** 0–9 for the number keys, otherwise null. */
    val digit: Int? get() = when (this) {
        D0 -> 0; D1 -> 1; D2 -> 2; D3 -> 3; D4 -> 4; D5 -> 5; D6 -> 6; D7 -> 7; D8 -> 8; D9 -> 9
        else -> null
    }
}
