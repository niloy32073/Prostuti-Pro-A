package com.prostuti.student.ui.screen.playground.graph.component

import com.prostuti.student.ui.screen.playground.graph.KeyboardPage
import com.prostuti.student.ui.screen.playground.graph.MathKeyAction
import com.prostuti.student.ui.screen.playground.graph.MathKeyAction.Insert

/** Key rows for each keyboard page. Text inserted is plain parser syntax the student can read. */
internal object MathKeyboardLayout {

    private fun k(label: String, text: String = label, back: Int = 0, style: KeyStyle = KeyStyle.PLAIN, weight: Float = 1f, id: String = label) =
        MathKey(label, Insert(text, back), style, weight, id)

    private fun digit(d: String) = k(d, style = KeyStyle.DIGIT)
    private fun op(label: String, text: String = label) = k(label, text, style = KeyStyle.OPERATOR)

    fun rows(page: KeyboardPage): List<List<MathKey>> = when (page) {
        KeyboardPage.MAIN -> listOf(
            listOf(k("x"), k("y"), k("a²", "^2", id = "square"), k("aᵇ", "^", id = "power"), digit("7"), digit("8"), digit("9"), op("÷")),
            listOf(k("("), k(")"), k("<"), k(">"), digit("4"), digit("5"), digit("6"), op("×")),
            listOf(k("|a|", "||", 1, id = "abs"), k(","), k("≤"), k("≥"), digit("1"), digit("2"), digit("3"), op("−")),
            listOf(k("√", "√()", 1, id = "sqrt"), k("π"), k("θ"), op("="), digit("0"), digit("."), k("e"), op("+")),
        )
        KeyboardPage.FUNCTIONS -> listOf(
            listOf(k("sin", "sin()", 1), k("cos", "cos()", 1), k("tan", "tan()", 1), k("sec", "sec()", 1), k("csc", "csc()", 1), k("cot", "cot()", 1)),
            listOf(
                k("sin⁻¹", "sin⁻¹()", 1), k("cos⁻¹", "cos⁻¹()", 1), k("tan⁻¹", "tan⁻¹()", 1),
                k("sinh", "sinh()", 1), k("cosh", "cosh()", 1), k("tanh", "tanh()", 1),
            ),
            listOf(
                k("ln", "ln()", 1), k("log", "log()", 1), k("logₐ", "log_()", 3, id = "log_base"),
                k("eˣ", "e^()", 1, id = "exp"), k("∛", "cbrt()", 1, id = "cbrt"), k("ⁿ√", "nthroot(, )", 3, id = "nthroot"),
            ),
            listOf(
                k("|x|", "abs()", 1, id = "abs_fn"), k("⌊x⌋", "floor()", 1, id = "floor"), k("⌈x⌉", "ceil()", 1, id = "ceil"),
                k("n!", "!", id = "factorial"), k("nCr", "nCr(, )", 3), k("mod", "mod(, )", 3),
            ),
        )
        KeyboardPage.CALCULUS -> listOf(
            listOf(
                k("∫ₐᵇ", "∫_(0)^(1)  dx", 3, id = "integral_definite"), k("∫", "∫  dx", 3, id = "integral"),
                k("d/dx", "d/dx()", 1, id = "derivative"), k("f′", "'", id = "prime"), k("Σ", "Σ_(n=1)^(10) ", id = "sum"),
            ),
            listOf(
                k("{ : }", "{x<0: , }", 3, id = "piecewise"), k("{a<x<b}", " {0<x<1}", id = "restriction"),
                k("∞"), k("∏", "∏_(k=1)^(5) ", id = "product"), k("min", "min(, )", 3),
            ),
            listOf(k("y ="), k("x ="), k("r ="), k("f(x) =", id = "define"), k("(a, b)", "(, )", 3, id = "point")),
            listOf(k("t"), k("θ"), k("r"), k("eˣ", "e^()", 1, id = "exp2"), k("max", "max(, )", 3)),
        )
        KeyboardPage.LETTERS -> listOf(
            "qwertyuiop".map { k(it.toString()) },
            "asdfghjkl".map { k(it.toString()) },
            "zxcvbnm".map { k(it.toString()) } + listOf(k("_", id = "subscript"), k("′", "'", id = "prime2")),
            listOf(k("θ"), k("π"), k("α"), k("β"), k("λ"), k("ω"), k("φ"), k("␣", " ", weight = 2f, id = "space")),
        )
    }

    fun bottomRow(page: KeyboardPage): List<MathKey> = listOf(
        MathKey("123", MathKeyAction.Page(KeyboardPage.MAIN), if (page == KeyboardPage.MAIN) KeyStyle.ACTION else KeyStyle.OPERATOR, id = "page_main"),
        MathKey("f(x)", MathKeyAction.Page(KeyboardPage.FUNCTIONS), if (page == KeyboardPage.FUNCTIONS) KeyStyle.ACTION else KeyStyle.OPERATOR, id = "page_functions"),
        MathKey("∫ dx", MathKeyAction.Page(KeyboardPage.CALCULUS), if (page == KeyboardPage.CALCULUS) KeyStyle.ACTION else KeyStyle.OPERATOR, id = "page_calculus"),
        MathKey("abc", MathKeyAction.Page(KeyboardPage.LETTERS), if (page == KeyboardPage.LETTERS) KeyStyle.ACTION else KeyStyle.OPERATOR, id = "page_letters"),
        MathKey("◀", MathKeyAction.Left, KeyStyle.OPERATOR, id = "left"),
        MathKey("▶", MathKeyAction.Right, KeyStyle.OPERATOR, id = "right"),
        MathKey("⌫", MathKeyAction.Backspace, KeyStyle.OPERATOR, id = "backspace"),
        MathKey("↵", MathKeyAction.Enter, KeyStyle.ACTION, id = "enter"),
    )
}
