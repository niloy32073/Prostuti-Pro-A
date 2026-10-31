package com.prostuti.student.ui.screen.examengine.calculator

import androidx.lifecycle.ViewModel
import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.calculator.CalcContext
import com.prostuti.student.domain.calculator.CalcErrorKind
import com.prostuti.student.domain.calculator.CalcException
import com.prostuti.student.domain.calculator.CalcNumberFormat
import com.prostuti.student.domain.calculator.CalcToken
import com.prostuti.student.domain.calculator.Complex
import com.prostuti.student.domain.calculator.EquationSolver
import com.prostuti.student.domain.calculator.ExpressionEvaluator
import com.prostuti.student.domain.calculator.LinearSolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The exam's scientific calculator. It has its own ViewModel (scoped to the exam's navigation
 * entry) so none of its state lives in [com.prostuti.student.ui.screen.examengine.ExamAttemptViewModel].
 * Every key press is a synchronous state transition; the maths lives in the domain engine.
 */
class CalculatorViewModel(
    private val evaluator: ExpressionEvaluator,
    private val equationSolver: EquationSolver,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    fun onKey(key: CalcKey) = _state.update { reduce(it, key) }

    // ── Key → action ─────────────────────────────────────────────────────────

    private sealed interface Action {
        data class Insert(val token: String) : Action
        data class Cursor(val key: CalcKey) : Action
        data class Eng(val step: Int) : Action
        data class OpenMenu(val menu: CalcMenu) : Action
        data object Delete : Action
        data object AllClear : Action
        data object Equals : Action
        data object Calc : Action
        data object Solve : Action
        data object Store : Action
        data object Recall : Action
        data object MemoryPlus : Action
        data object MemoryMinus : Action
        data object ToggleDecimal : Action
        data object Sexagesimal : Action
        data object ToggleHyp : Action
        data object None : Action
    }

    private fun resolve(key: CalcKey, shift: Boolean, alpha: Boolean, hyp: Boolean): Action {
        if (alpha) {
            ALPHA_VARIABLES[key]?.let { return Action.Insert(CalcToken.variable(it)) }
            return when (key) {
                CalcKey.CALC -> Action.Insert(CalcToken.EQUALS)
                CalcKey.EXP10 -> Action.Insert(CalcToken.E)
                else -> Action.None
            }
        }
        if (shift) return when (key) {
            CalcKey.MODE -> Action.OpenMenu(CalcMenu.SETUP)
            CalcKey.CALC -> Action.Solve
            CalcKey.INTEGRAL -> Action.Insert(CalcToken.DERIVATIVE)
            CalcKey.INVERSE -> Action.Insert(CalcToken.FACTORIAL)
            CalcKey.LOG_AB -> Action.Insert(CalcToken.SUM)
            CalcKey.SQRT -> Action.Insert(CalcToken.CBRT)
            CalcKey.SQUARE -> Action.Insert(CalcToken.CUBE)
            CalcKey.POWER -> Action.Insert(CalcToken.XROOT)
            CalcKey.LOG -> Action.Insert(CalcToken.POW10)
            CalcKey.LN -> Action.Insert(CalcToken.EXP)
            CalcKey.HYP -> Action.Insert(CalcToken.ABS)
            CalcKey.SIN -> Action.Insert(if (hyp) CalcToken.ASINH else CalcToken.ASIN)
            CalcKey.COS -> Action.Insert(if (hyp) CalcToken.ACOSH else CalcToken.ACOS)
            CalcKey.TAN -> Action.Insert(if (hyp) CalcToken.ATANH else CalcToken.ATAN)
            CalcKey.RCL -> Action.Store
            CalcKey.ENG -> Action.Eng(+1)
            CalcKey.OPEN -> Action.Insert(CalcToken.PERCENT)
            CalcKey.CLOSE -> Action.Insert(CalcToken.COMMA)
            CalcKey.M_PLUS -> Action.MemoryMinus
            CalcKey.D9 -> Action.OpenMenu(CalcMenu.CLEAR)
            CalcKey.MULTIPLY -> Action.Insert(CalcToken.NPR)
            CalcKey.DIVIDE -> Action.Insert(CalcToken.NCR)
            CalcKey.D0 -> Action.Insert(CalcToken.RND)
            CalcKey.DOT -> Action.Insert(CalcToken.RANDOM)
            CalcKey.EXP10 -> Action.Insert(CalcToken.PI)
            CalcKey.ANS -> Action.OpenMenu(CalcMenu.DRG)
            CalcKey.AC -> Action.AllClear
            CalcKey.EQUALS -> Action.Equals
            CalcKey.LEFT, CalcKey.RIGHT, CalcKey.UP, CalcKey.DOWN -> Action.Cursor(key)
            else -> Action.None
        }
        key.digit?.let { return Action.Insert(it.toString()) }
        return when (key) {
            CalcKey.MODE -> Action.OpenMenu(CalcMenu.MODE)
            CalcKey.CALC -> Action.Calc
            CalcKey.INTEGRAL -> Action.Insert(CalcToken.INTEGRAL)
            CalcKey.INVERSE -> Action.Insert(CalcToken.INVERSE)
            CalcKey.LOG_AB -> Action.Insert(CalcToken.LOG)
            CalcKey.FRACTION -> Action.Insert(CalcToken.FRACTION)
            CalcKey.SQRT -> Action.Insert(CalcToken.SQRT)
            CalcKey.SQUARE -> Action.Insert(CalcToken.SQUARE)
            CalcKey.POWER -> Action.Insert(CalcToken.POWER)
            CalcKey.LOG -> Action.Insert(CalcToken.LOG)
            CalcKey.LN -> Action.Insert(CalcToken.LN)
            CalcKey.NEGATIVE -> Action.Insert(CalcToken.NEG)
            CalcKey.DMS -> Action.Sexagesimal
            CalcKey.HYP -> Action.ToggleHyp
            CalcKey.SIN -> Action.Insert(if (hyp) CalcToken.SINH else CalcToken.SIN)
            CalcKey.COS -> Action.Insert(if (hyp) CalcToken.COSH else CalcToken.COS)
            CalcKey.TAN -> Action.Insert(if (hyp) CalcToken.TANH else CalcToken.TAN)
            CalcKey.RCL -> Action.Recall
            CalcKey.ENG -> Action.Eng(-1)
            CalcKey.OPEN -> Action.Insert(CalcToken.OPEN)
            CalcKey.CLOSE -> Action.Insert(CalcToken.CLOSE)
            CalcKey.S_D -> Action.ToggleDecimal
            CalcKey.M_PLUS -> Action.MemoryPlus
            CalcKey.DEL -> Action.Delete
            CalcKey.AC -> Action.AllClear
            CalcKey.MULTIPLY -> Action.Insert(CalcToken.TIMES)
            CalcKey.DIVIDE -> Action.Insert(CalcToken.DIVIDE)
            CalcKey.PLUS -> Action.Insert(CalcToken.PLUS)
            CalcKey.MINUS -> Action.Insert(CalcToken.MINUS)
            CalcKey.DOT -> Action.Insert(CalcToken.DOT)
            CalcKey.EXP10 -> Action.Insert(CalcToken.EXP10)
            CalcKey.ANS -> Action.Insert(CalcToken.ANS)
            CalcKey.EQUALS -> Action.Equals
            CalcKey.LEFT, CalcKey.RIGHT, CalcKey.UP, CalcKey.DOWN -> Action.Cursor(key)
            else -> Action.None
        }
    }

    // ── Dispatch ─────────────────────────────────────────────────────────────

    private fun reduce(s: CalculatorUiState, key: CalcKey): CalculatorUiState {
        when (key) {
            CalcKey.SHIFT -> return s.copy(shift = !s.shift, alpha = false)
            CalcKey.ALPHA -> return s.copy(alpha = !s.alpha, shift = false)
            CalcKey.ON -> return s.copy(
                shift = false, alpha = false, hyp = false, memoryCommand = null,
                editor = ExpressionEditor(), display = CalcDisplay.Input, prompt = null, historyIndex = -1,
                equation = s.equation?.copy(roots = null),
            )
            else -> Unit
        }
        val action = resolve(key, s.shift, s.alpha, s.hyp)
        val base = s.copy(shift = false, alpha = false, hyp = action == Action.ToggleHyp && !s.hyp)
        if (action == Action.ToggleHyp) return base

        s.memoryCommand?.let { return memoryCommand(base.copy(memoryCommand = null), it, key) }

        return when (val display = s.display) {
            is CalcDisplay.Menu -> menu(base, display.menu, key)
            is CalcDisplay.ConfirmClear -> confirmClear(base, display.target, action)
            is CalcDisplay.Error -> error(base, display, action)
            is CalcDisplay.Message ->
                if (action == Action.AllClear || action == Action.Equals) base.copy(display = CalcDisplay.Input) else base
            else -> when {
                s.mode == CalcMode.EQN -> equation(base, action)
                s.prompt != null -> prompt(base, s.prompt, action)
                else -> compute(base, action)
            }
        }
    }

    // ── COMP mode ────────────────────────────────────────────────────────────

    private fun compute(s: CalculatorUiState, action: Action): CalculatorUiState {
        val result = s.display as? CalcDisplay.Result
        return when (action) {
            is Action.Insert -> insert(s, action.token)
            Action.Sexagesimal ->
                if (result != null) s.copy(display = result.copy(sexagesimal = !result.sexagesimal))
                else insert(s, CalcToken.DMS)
            Action.Delete ->
                if (result != null) s.copy(display = CalcDisplay.Input, editor = ExpressionEditor(result.expression))
                else s.copy(editor = s.editor.delete())
            is Action.Cursor -> cursor(s, action.key)
            Action.AllClear -> s.copy(editor = ExpressionEditor(), display = CalcDisplay.Input, historyIndex = -1)
            Action.Equals -> equals(s)
            is Action.OpenMenu -> s.copy(display = CalcDisplay.Menu(action.menu))
            Action.ToggleDecimal -> when {
                result != null -> s.copy(display = result.copy(asDecimal = !result.asDecimal))
                else -> equals(s).let { done ->
                    val shown = done.display as? CalcDisplay.Result
                    if (shown != null) done.copy(display = shown.copy(asDecimal = true)) else done
                }
            }
            is Action.Eng ->
                if (result != null) s.copy(display = result.copy(engineeringStep = result.engineeringStep?.plus(action.step) ?: 0))
                else s
            Action.Calc -> startPrompt(s, PromptPurpose.CALC)
            Action.Solve -> startPrompt(s, PromptPurpose.SOLVE)
            Action.Store -> s.copy(memoryCommand = MemoryCommand.STORE)
            Action.Recall -> s.copy(memoryCommand = MemoryCommand.RECALL)
            Action.MemoryPlus -> addToMemory(s, +1.0)
            Action.MemoryMinus -> addToMemory(s, -1.0)
            Action.ToggleHyp, Action.None -> s
        }
    }

    /** After a result, typing starts a new expression; an operator continues from Ans. */
    private fun insert(s: CalculatorUiState, token: String): CalculatorUiState {
        if (s.display is CalcDisplay.Result) {
            val start = if (token in CalcToken.CONTINUES_ANSWER) ExpressionEditor(listOf(CalcToken.ANS)) else ExpressionEditor()
            return s.copy(editor = start.insert(token), display = CalcDisplay.Input, historyIndex = -1)
        }
        return s.copy(editor = s.editor.insert(token), display = CalcDisplay.Input)
    }

    private fun cursor(s: CalculatorUiState, key: CalcKey): CalculatorUiState {
        val result = s.display as? CalcDisplay.Result
        return when (key) {
            CalcKey.LEFT, CalcKey.RIGHT -> when {
                result != null -> {
                    val editor = ExpressionEditor(result.expression)
                    s.copy(display = CalcDisplay.Input, editor = if (key == CalcKey.LEFT) editor else editor.copy(cursor = 0))
                }
                key == CalcKey.LEFT -> s.copy(editor = s.editor.moveLeft())
                else -> s.copy(editor = s.editor.moveRight())
            }
            CalcKey.UP -> if (result != null || s.editor.isEmpty) showHistory(s, s.historyIndex + 1) else s
            CalcKey.DOWN -> if (result != null && s.historyIndex > 0) showHistory(s, s.historyIndex - 1) else s
            else -> s
        }
    }

    private fun showHistory(s: CalculatorUiState, index: Int): CalculatorUiState {
        if (s.history.isEmpty()) return s
        val i = index.coerceIn(0, s.history.lastIndex)
        val entry = s.history[s.history.lastIndex - i]
        return s.copy(
            historyIndex = i,
            editor = ExpressionEditor(entry.expression),
            display = CalcDisplay.Result(entry.expression, entry.value),
        )
    }

    private fun equals(s: CalculatorUiState): CalculatorUiState {
        val result = s.display as? CalcDisplay.Result
        val tokens = when {
            result != null -> result.expression
            s.editor.isEmpty -> s.history.lastOrNull()?.expression.orEmpty()
            else -> s.editor.tokens
        }
        if (tokens.isEmpty()) return s
        return evaluateAndShow(s, tokens)
    }

    private fun evaluateAndShow(s: CalculatorUiState, tokens: List<String>): CalculatorUiState = try {
        val value = evaluator.evaluate(tokens, s.context())
        s.copy(
            ans = value,
            history = (s.history + CalcHistoryEntry(tokens, value)).takeLast(MAX_HISTORY),
            historyIndex = 0,
            editor = ExpressionEditor(tokens),
            display = CalcDisplay.Result(tokens, value),
            prompt = null,
        )
    } catch (e: CalcException) {
        s.copy(display = CalcDisplay.Error(e.kind, e.position), editor = ExpressionEditor(tokens), prompt = null)
    }

    private fun addToMemory(s: CalculatorUiState, sign: Double): CalculatorUiState {
        val result = s.display as? CalcDisplay.Result
        val (tokens, value) = when {
            result != null -> result.expression to result.value
            s.editor.isEmpty -> return s
            else -> try {
                s.editor.tokens to evaluator.evaluate(s.editor.tokens, s.context())
            } catch (e: CalcException) {
                return s.copy(display = CalcDisplay.Error(e.kind, e.position))
            }
        }
        val memory = (s.variables["M"] ?: 0.0) + sign * value
        return s.copy(
            variables = s.variables + ("M" to memory),
            ans = value,
            editor = ExpressionEditor(tokens),
            display = CalcDisplay.Result(tokens, value, suffix = if (sign > 0) "M+" else "M−"),
        )
    }

    private fun memoryCommand(s: CalculatorUiState, command: MemoryCommand, key: CalcKey): CalculatorUiState {
        if (s.mode != CalcMode.COMP) return s
        val name = ALPHA_VARIABLES[key] ?: return s
        return when (command) {
            MemoryCommand.STORE -> {
                val result = s.display as? CalcDisplay.Result
                val (tokens, value) = when {
                    result != null -> result.expression to result.value
                    s.editor.isEmpty -> listOf(CalcToken.ANS) to s.ans
                    else -> try {
                        s.editor.tokens to evaluator.evaluate(s.editor.tokens, s.context())
                    } catch (e: CalcException) {
                        return s.copy(display = CalcDisplay.Error(e.kind, e.position))
                    }
                }
                s.copy(
                    variables = s.variables + (name to value),
                    ans = value,
                    editor = ExpressionEditor(tokens),
                    display = CalcDisplay.Result(tokens, value, suffix = "→$name"),
                )
            }
            MemoryCommand.RECALL -> {
                val value = s.variables[name] ?: 0.0
                val tokens = listOf(CalcToken.variable(name))
                s.copy(ans = value, editor = ExpressionEditor(tokens), display = CalcDisplay.Result(tokens, value, label = "$name="))
            }
        }
    }

    // ── CALC / SOLVE prompts ─────────────────────────────────────────────────

    private fun startPrompt(s: CalculatorUiState, purpose: PromptPurpose): CalculatorUiState {
        val tokens = (s.display as? CalcDisplay.Result)?.expression ?: s.editor.tokens
        if (tokens.isEmpty()) return s
        val used = evaluator.variablesIn(tokens)
        val variables = when (purpose) {
            PromptPurpose.CALC -> used
            PromptPurpose.SOLVE -> {
                val target = if ("X" in used) "X" else used.firstOrNull()
                    ?: return s.copy(display = CalcDisplay.Error(CalcErrorKind.CANT_SOLVE), editor = ExpressionEditor(tokens))
                used.filter { it != target } + target
            }
        }
        if (variables.isEmpty()) return evaluateAndShow(s, tokens)
        return s.copy(
            prompt = VariablePrompt(purpose, tokens, variables),
            editor = ExpressionEditor(),
            display = CalcDisplay.Input,
        )
    }

    private fun prompt(s: CalculatorUiState, p: VariablePrompt, action: Action): CalculatorUiState = when (action) {
        is Action.Insert -> s.copy(editor = s.editor.insert(action.token))
        Action.Sexagesimal -> s.copy(editor = s.editor.insert(CalcToken.DMS))
        Action.Delete -> s.copy(editor = s.editor.delete())
        is Action.Cursor -> when (action.key) {
            CalcKey.LEFT -> s.copy(editor = s.editor.moveLeft())
            CalcKey.RIGHT -> s.copy(editor = s.editor.moveRight())
            else -> s
        }
        Action.AllClear -> s.copy(prompt = null, editor = ExpressionEditor(p.expression), display = CalcDisplay.Input)
        is Action.OpenMenu -> s.copy(prompt = null, editor = ExpressionEditor(p.expression), display = CalcDisplay.Menu(action.menu))
        Action.Equals -> promptValue(s, p)
        else -> s
    }

    private fun promptValue(s: CalculatorUiState, p: VariablePrompt): CalculatorUiState {
        val value = if (s.editor.isEmpty) {
            s.variables[p.current] ?: 0.0
        } else {
            try {
                evaluator.evaluate(s.editor.tokens, s.context())
            } catch (e: CalcException) {
                return s.copy(prompt = null, editor = ExpressionEditor(p.expression), display = CalcDisplay.Error(e.kind))
            }
        }
        val withValue = s.copy(variables = s.variables + (p.current to value), editor = ExpressionEditor())
        if (!p.isLast) return withValue.copy(prompt = p.copy(index = p.index + 1))

        return when (p.purpose) {
            PromptPurpose.CALC -> evaluateAndShow(withValue, p.expression)
            PromptPurpose.SOLVE -> try {
                val outcome = evaluator.solve(p.expression, p.current, value, withValue.context())
                withValue.copy(
                    variables = withValue.variables + (p.current to outcome.value),
                    prompt = null,
                    editor = ExpressionEditor(p.expression),
                    display = CalcDisplay.Result(p.expression, outcome.value, label = "${p.current}=", residual = outcome.leftMinusRight),
                )
            } catch (e: CalcException) {
                withValue.copy(prompt = null, editor = ExpressionEditor(p.expression), display = CalcDisplay.Error(e.kind, e.position))
            }
        }
    }

    // ── EQN mode ─────────────────────────────────────────────────────────────

    private fun equation(s: CalculatorUiState, action: Action): CalculatorUiState {
        val session = s.equation ?: return s.copy(mode = CalcMode.COMP)
        if (action is Action.OpenMenu) return s.copy(display = CalcDisplay.Menu(action.menu))

        val roots = session.roots
        if (roots != null) {
            val last = roots.lastIndex
            val next = when {
                action == Action.Equals ->
                    if (session.rootIndex < last) session.copy(rootIndex = session.rootIndex + 1) else session.copy(roots = null)
                action == Action.Cursor(CalcKey.DOWN) -> session.copy(rootIndex = (session.rootIndex + 1).coerceAtMost(last))
                action == Action.Cursor(CalcKey.UP) -> session.copy(rootIndex = (session.rootIndex - 1).coerceAtLeast(0))
                action == Action.AllClear -> session.copy(roots = null)
                action == Action.ToggleDecimal -> session.copy(rootsAsDecimal = !session.rootsAsDecimal)
                else -> session
            }
            return s.copy(equation = next)
        }

        return when (action) {
            is Action.Insert -> s.copy(editor = s.editor.insert(action.token))
            Action.Sexagesimal -> s.copy(editor = s.editor.insert(CalcToken.DMS))
            Action.Delete -> s.copy(editor = s.editor.delete())
            Action.AllClear -> s.copy(editor = ExpressionEditor())
            is Action.Cursor -> when {
                !s.editor.isEmpty && action.key == CalcKey.LEFT -> s.copy(editor = s.editor.moveLeft())
                !s.editor.isEmpty && action.key == CalcKey.RIGHT -> s.copy(editor = s.editor.moveRight())
                !s.editor.isEmpty -> s
                else -> s.copy(
                    equation = when (action.key) {
                        CalcKey.UP -> session.move(-1, 0)
                        CalcKey.DOWN -> session.move(1, 0)
                        CalcKey.LEFT -> session.move(0, -1)
                        else -> session.move(0, 1)
                    }
                )
            }
            Action.Equals ->
                if (s.editor.isEmpty) {
                    solveEquation(s, session)
                } else {
                    try {
                        val value = evaluator.evaluate(s.editor.tokens, s.context())
                        s.copy(equation = session.withValue(value).advance(), editor = ExpressionEditor())
                    } catch (e: CalcException) {
                        s.copy(display = CalcDisplay.Error(e.kind, e.position))
                    }
                }
            else -> s
        }
    }

    private fun solveEquation(s: CalculatorUiState, session: EquationSession): CalculatorUiState {
        val roots: List<EquationRoot> = if (session.type.isLinear) {
            when (val solution = equationSolver.solveLinear(session.coefficients)) {
                is LinearSolution.Unique -> solution.values.mapIndexed { i, v -> EquationRoot("${LINEAR_NAMES[i]}=", Complex(v)) }
                LinearSolution.NoSolution -> return s.copy(display = CalcDisplay.Error(CalcErrorKind.NO_SOLUTION))
                LinearSolution.InfiniteSolutions -> return s.copy(display = CalcDisplay.Error(CalcErrorKind.INFINITE_SOLUTION))
            }
        } else {
            try {
                equationSolver.solvePolynomial(session.coefficients.first())
                    .mapIndexed { i, z -> EquationRoot("X${i + 1}=", z) }
            } catch (e: CalcException) {
                return s.copy(display = CalcDisplay.Error(e.kind))
            }
        }
        return s.copy(equation = session.copy(roots = roots, rootIndex = 0, rootsAsDecimal = false))
    }

    // ── Menus, errors ────────────────────────────────────────────────────────

    private fun menu(s: CalculatorUiState, menu: CalcMenu, key: CalcKey): CalculatorUiState {
        val toInput = s.copy(display = CalcDisplay.Input)
        if (key == CalcKey.AC) return toInput
        if (key == CalcKey.MODE) return if (menu == CalcMenu.MODE) toInput else s.copy(display = CalcDisplay.Menu(CalcMenu.MODE))
        val n = key.digit ?: return s
        return when (menu) {
            CalcMenu.MODE -> when (n) {
                1 -> toInput.copy(mode = CalcMode.COMP, equation = null, editor = ExpressionEditor(), prompt = null)
                2 -> s.copy(display = CalcDisplay.Menu(CalcMenu.EQUATION_TYPE))
                else -> s
            }
            CalcMenu.EQUATION_TYPE -> EquationType.entries.getOrNull(n - 1)?.let {
                toInput.copy(mode = CalcMode.EQN, equation = EquationSession(it), editor = ExpressionEditor(), prompt = null)
            } ?: s
            CalcMenu.SETUP -> when (n) {
                1 -> toInput.copy(angleUnit = AngleUnit.DEG)
                2 -> toInput.copy(angleUnit = AngleUnit.RAD)
                3 -> toInput.copy(angleUnit = AngleUnit.GRAD)
                4 -> s.copy(display = CalcDisplay.Menu(CalcMenu.FIX))
                5 -> s.copy(display = CalcDisplay.Menu(CalcMenu.SCI))
                6 -> s.copy(display = CalcDisplay.Menu(CalcMenu.NORM))
                else -> s
            }
            CalcMenu.FIX -> toInput.copy(numberFormat = CalcNumberFormat.Fix(n))
            CalcMenu.SCI -> toInput.copy(numberFormat = CalcNumberFormat.Sci(if (n == 0) 10 else n))
            CalcMenu.NORM -> if (n == 1 || n == 2) toInput.copy(numberFormat = CalcNumberFormat.Norm(n)) else s
            CalcMenu.DRG -> when (n) {
                1 -> toInput.copy(editor = s.editor.insert(CalcToken.UNIT_DEG))
                2 -> toInput.copy(editor = s.editor.insert(CalcToken.UNIT_RAD))
                3 -> toInput.copy(editor = s.editor.insert(CalcToken.UNIT_GRAD))
                else -> s
            }
            CalcMenu.CLEAR -> when (n) {
                1 -> s.copy(display = CalcDisplay.ConfirmClear(ClearTarget.SETUP))
                2 -> s.copy(display = CalcDisplay.ConfirmClear(ClearTarget.MEMORY))
                3 -> s.copy(display = CalcDisplay.ConfirmClear(ClearTarget.ALL))
                else -> s
            }
        }
    }

    private fun confirmClear(s: CalculatorUiState, target: ClearTarget, action: Action): CalculatorUiState = when (action) {
        Action.Equals -> {
            val message = CalcDisplay.Message(target.done)
            when (target) {
                ClearTarget.SETUP -> s.copy(angleUnit = AngleUnit.DEG, numberFormat = CalcNumberFormat.Norm(1), display = message)
                ClearTarget.MEMORY -> s.copy(variables = CalculatorUiState().variables, ans = 0.0, display = message)
                ClearTarget.ALL -> CalculatorUiState(display = message)
            }
        }
        Action.AllClear -> s.copy(display = CalcDisplay.Input)
        else -> s
    }

    private fun error(s: CalculatorUiState, error: CalcDisplay.Error, action: Action): CalculatorUiState = when {
        action == Action.AllClear -> s.copy(
            display = CalcDisplay.Input,
            editor = ExpressionEditor(),
            prompt = null,
            equation = s.equation?.copy(roots = null),
        )
        action == Action.Cursor(CalcKey.LEFT) || action == Action.Cursor(CalcKey.RIGHT) -> s.copy(
            display = CalcDisplay.Input,
            editor = s.editor.copy(cursor = error.position?.coerceIn(0, s.editor.tokens.size) ?: s.editor.tokens.size),
        )
        action is Action.OpenMenu -> s.copy(display = CalcDisplay.Menu(action.menu), editor = ExpressionEditor())
        else -> s
    }

    private fun CalculatorUiState.context() = CalcContext(variables = variables, ans = ans, angleUnit = angleUnit)

    private companion object {
        const val MAX_HISTORY = 30
        val LINEAR_NAMES = listOf("X", "Y", "Z", "T")

        /** ALPHA (and STO / RCL) letters printed in red above these keys. */
        val ALPHA_VARIABLES = mapOf(
            CalcKey.NEGATIVE to "A", CalcKey.DMS to "B", CalcKey.HYP to "C",
            CalcKey.SIN to "D", CalcKey.COS to "E", CalcKey.TAN to "F",
            CalcKey.CLOSE to "X", CalcKey.S_D to "Y", CalcKey.M_PLUS to "M",
        )
    }
}
