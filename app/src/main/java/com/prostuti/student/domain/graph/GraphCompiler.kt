package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import kotlin.math.E
import kotlin.math.PI

/**
 * Compiles the whole expression list at once, because lines depend on each other:
 * `a = 2` feeds `y = a x²`, `f(x) = x³` feeds `y = f'(x)`.
 *
 * Steps: find function names → parse every line → collect definitions (and duplicates) → evaluate
 * parameters in dependency order (cycles are errors) → inline functions → expand d/dx →
 * classify each line into a [GraphPlot] and compile it.
 *
 * Not thread-safe (it keeps a parse cache); use one instance per caller.
 */
class GraphCompiler {

    private val parseCache = object : LinkedHashMap<String, ParseResult>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParseResult>?): Boolean = size > 400
    }

    fun compile(inputs: List<GraphLineInput>, angleUnit: AngleUnit): CompiledGraph = Run(inputs, angleUnit).compile()

    private fun parse(text: String, functions: Set<String>): ParseResult {
        val key = text + '\u0000' + functions.sorted().joinToString(",")
        return parseCache.getOrPut(key) { MathParser.parse(text, functions) }
    }

    private class FunctionDef(val lineId: Long, val name: String, val params: List<String>, val body: MathNode)
    private class Assignment(val lineId: Long, val name: String, val rhs: MathNode)

    private inner class Run(private val inputs: List<GraphLineInput>, private val angleUnit: AngleUnit) {
        private val differentiator = Differentiator(angleUnit)
        private var fresh = 0

        private val functionNames: Set<String> = inputs.mapNotNull { definedFunctionName(it.text) }.toSet()
        private val parsed: Map<Long, ParseResult> = inputs.associate { it.id to parse(it.text, functionNames) }

        private val functions = HashMap<String, FunctionDef>()
        private val assignments = HashMap<String, Assignment>()
        private val lineErrors = HashMap<Long, GraphError>()

        private val paramValues = LinkedHashMap<String, Double>()
        private val brokenNames = HashSet<String>()
        private val constants = HashMap<String, Double>(BUILTIN_CONSTANTS)

        fun compile(): CompiledGraph {
            collectDefinitions()
            resolveParameters()
            constants.putAll(paramValues)
            val lines = inputs.map { input -> safely(input) { line(input) } }
            return CompiledGraph(lines, paramValues.toMap(), angleUnit)
        }

        private fun safely(input: GraphLineInput, block: () -> CompiledLine): CompiledLine {
            val latex = (parsed[input.id] as? ParseResult.Success)?.let { LatexWriter.write(it.statement) }
            return try {
                block()
            } catch (e: CompileException) {
                CompiledLine(input.id, GraphLineKind.ERROR, latex, e.error)
            } catch (e: StackOverflowError) {
                CompiledLine(input.id, GraphLineKind.ERROR, latex, GraphError(GraphErrorKind.TOO_COMPLEX))
            }
        }

        // ── Definitions ─────────────────────────────────────────────────────────────────────

        private fun collectDefinitions() {
            val byName = HashMap<String, MutableList<Long>>()
            for (input in inputs) {
                val statement = (parsed[input.id] as? ParseResult.Success)?.statement ?: continue
                if (statement.relations != listOf(Relation.EQ)) continue
                val left = statement.operands[0]
                val right = statement.operands[1]
                when {
                    left is MathNode.Call && left.name in functionNames && left.primes == 0 -> {
                        val params = left.args.map { (it as? MathNode.Sym)?.name }
                        if (params.any { it == null || it in BUILTIN_CONSTANTS } || params.toSet().size != params.size) {
                            lineErrors[input.id] = GraphError(GraphErrorKind.INVALID_DEFINITION)
                        } else {
                            functions[left.name] = FunctionDef(input.id, left.name, params.filterNotNull(), right)
                        }
                        byName.getOrPut(left.name) { mutableListOf() } += input.id
                    }
                    left is MathNode.Sym && left.name !in RESERVED_VARIABLES -> {
                        if (left.name in BUILTIN_CONSTANTS) {
                            lineErrors[input.id] = GraphError(GraphErrorKind.RESERVED_NAME, names = listOf(left.name))
                        } else {
                            assignments[left.name] = Assignment(input.id, left.name, right)
                            byName.getOrPut(left.name) { mutableListOf() } += input.id
                        }
                    }
                }
            }
            for ((name, ids) in byName) {
                if (ids.size > 1) {
                    ids.forEach { lineErrors[it] = GraphError(GraphErrorKind.DUPLICATE_DEFINITION, names = listOf(name)) }
                    functions.remove(name)
                    assignments.remove(name)
                    brokenNames += name
                }
            }
            for ((id, _) in lineErrors) {
                functions.values.firstOrNull { it.lineId == id }?.let { functions.remove(it.name); brokenNames += it.name }
            }
            functionNames.filter { it !in functions }.forEach { brokenNames += it }
        }

        private val evaluating = HashSet<String>()

        private fun resolveParameters() {
            for (name in assignments.keys.toList()) {
                try {
                    parameterValue(name)
                } catch (e: CompileException) {
                    val line = assignments.getValue(name).lineId
                    lineErrors.putIfAbsent(line, e.error)
                    brokenNames += name
                }
            }
        }

        private fun parameterValue(name: String): Double {
            paramValues[name]?.let { return it }
            if (name in brokenNames) throw CompileException(GraphError(GraphErrorKind.BROKEN_DEPENDENCY, names = listOf(name)))
            if (!evaluating.add(name)) throw CompileException(GraphError(GraphErrorKind.CIRCULAR_DEFINITION, names = listOf(name)))
            try {
                val rhs = prepare(assignments.getValue(name).rhs)
                val free = MathTree.freeSymbols(rhs)
                if (free.any { it in RESERVED_VARIABLES }) {
                    throw CompileException(GraphError(GraphErrorKind.INVALID_DEFINITION, names = listOf(name)))
                }
                val undefined = free.filter { it !in BUILTIN_CONSTANTS && it !in assignments && it !in brokenNames }
                if (undefined.isNotEmpty()) {
                    throw CompileException(GraphError(GraphErrorKind.UNDEFINED_VARIABLE, names = undefined.sorted()))
                }
                for (dep in free) if (dep in assignments || dep in brokenNames) constants[dep] = parameterValue(dep)
                val value = ExpressionCompiler(angleUnit, constants).compile(rhs, emptyList()).evaluate()
                paramValues[name] = value
                constants[name] = value
                return value
            } finally {
                evaluating.remove(name)
            }
        }

        // ── Rewrites ────────────────────────────────────────────────────────────────────────

        /** Inline user functions, then expand d/dx. */
        private fun prepare(node: MathNode): MathNode = differentiator.expand(inline(node, emptySet()))

        private fun inline(node: MathNode, stack: Set<String>): MathNode = MathTree.rewrite(node) { n ->
            if (n is MathNode.Call && !MathFunctions.isBuiltin(n.name)) expandCall(n, stack) else n
        }

        private fun expandCall(call: MathNode.Call, stack: Set<String>): MathNode {
            if (call.name in brokenNames || call.name !in functions) {
                throw CompileException(GraphError(GraphErrorKind.BROKEN_DEPENDENCY, names = listOf(call.name)))
            }
            if (call.name in stack) throw CompileException(GraphError(GraphErrorKind.CIRCULAR_DEFINITION, names = listOf(call.name)))
            val def = functions.getValue(call.name)
            if (def.params.size != call.args.size) throw CompileException(GraphError(GraphErrorKind.WRONG_ARGUMENT_COUNT, names = listOf(call.name)))
            var body = inline(def.body, stack + call.name)
            if (call.primes > 0) {
                if (def.params.size != 1) throw CompileException(GraphError(GraphErrorKind.WRONG_ARGUMENT_COUNT, names = listOf(call.name)))
                repeat(call.primes) { body = MathNode.Diff(def.params[0], body) }
            }
            val freshNames = def.params.map { "$it#${fresh++}" }
            def.params.zip(freshNames).forEach { (from, to) -> body = MathTree.renameFree(body, from, to) }
            for (i in def.params.indices.reversed()) body = MathNode.Let(freshNames[i], call.args[i], body)
            return body
        }

        /** Reserved variables the node uses; throws when it uses something nothing defines. */
        private fun variablesOf(node: MathNode): Set<String> {
            val free = MathTree.freeSymbols(node)
            val broken = free.filter { it in brokenNames }
            if (broken.isNotEmpty()) throw CompileException(GraphError(GraphErrorKind.BROKEN_DEPENDENCY, names = broken))
            val undefined = free.filter { it !in RESERVED_VARIABLES && it !in constants }
            if (undefined.isNotEmpty()) throw CompileException(GraphError(GraphErrorKind.UNDEFINED_VARIABLE, names = undefined.sorted()))
            return free.filterTo(LinkedHashSet()) { it in RESERVED_VARIABLES }
        }

        private fun compiler(polar: Boolean = false) = ExpressionCompiler(angleUnit, constants, polar)

        private fun fn(node: MathNode, vararg variables: String, polar: Boolean = false): CompiledFunction =
            compiler(polar).compile(node, variables.toList())

        // ── Lines ───────────────────────────────────────────────────────────────────────────

        private fun line(input: GraphLineInput): CompiledLine {
            val id = input.id
            if (input.text.isBlank()) return CompiledLine(id, GraphLineKind.EMPTY, null)
            val statement = when (val p = parsed.getValue(id)) {
                is ParseResult.Failure -> return CompiledLine(id, GraphLineKind.ERROR, null, p.error)
                is ParseResult.Success -> p.statement
            }
            val latex = LatexWriter.write(statement)
            lineErrors[id]?.let { return CompiledLine(id, GraphLineKind.ERROR, latex, it) }

            assignments.values.firstOrNull { it.lineId == id }?.let { a ->
                val value = paramValues[a.name]
                    ?: return CompiledLine(id, GraphLineKind.ERROR, latex, GraphError(GraphErrorKind.BROKEN_DEPENDENCY))
                val slider = a.rhs is MathNode.Num || (a.rhs is MathNode.Neg && a.rhs.operand is MathNode.Num)
                return CompiledLine(id, GraphLineKind.PARAMETER, latex, value = value, parameter = ParameterInfo(a.name, value, slider))
            }
            functions.values.firstOrNull { it.lineId == id }?.let { return functionLine(id, latex, it) }
            return statementLine(id, latex, statement)
        }

        private fun functionLine(id: Long, latex: String, def: FunctionDef): CompiledLine {
            val body = differentiator.expand(inline(def.body, setOf(def.name)))
            val free = MathTree.freeSymbols(body) - def.params.toSet()
            val broken = free.filter { it in brokenNames }
            if (broken.isNotEmpty()) throw CompileException(GraphError(GraphErrorKind.BROKEN_DEPENDENCY, names = broken))
            val undefined = free.filter { it !in RESERVED_VARIABLES && it !in constants }
            if (undefined.isNotEmpty()) throw CompileException(GraphError(GraphErrorKind.UNDEFINED_VARIABLE, names = undefined.sorted()))
            // Only one-input functions that use nothing but their input are drawn as y = f(x).
            val plot = if (def.params.size == 1 && free.none { it in RESERVED_VARIABLES }) explicit(body, def.params[0]) else null
            return CompiledLine(id, GraphLineKind.FUNCTION_DEFINITION, latex, plot = plot)
        }

        private fun explicit(body: MathNode, variable: String = "x"): GraphPlot.ExplicitY {
            val f = fn(body, variable)
            val derivative = runCatching { fn(differentiator.derivative(body, variable), variable) }.getOrNull()
            return GraphPlot.ExplicitY(f, derivative)
        }

        private fun statementLine(id: Long, latex: String, statement: MathStatement): CompiledLine {
            val ops = statement.operands.map(::prepare)
            val isTuple = ops.size == 1 && ops[0] is MathNode.Tuple
            if (!isTuple && ops.any(MathTree::containsTuple)) throw unsupported(GraphErrorKind.TUPLE_NOT_ALLOWED)
            val vars = ops.map(::variablesOf)
            if (vars.any { "z" in it } || (isTuple && (ops[0] as MathNode.Tuple).items.size == 3)) {
                return CompiledLine(id, GraphLineKind.ERROR, latex, GraphError(GraphErrorKind.NEEDS_3D))
            }
            return when (statement.relations.size) {
                0 -> bare(id, latex, ops[0], vars[0])
                1 -> relation(id, latex, ops[0], statement.relations[0], ops[1], vars[0], vars[1])
                2 -> {
                    val (r1, r2) = statement.relations
                    if (!r1.isInequality || !r2.isInequality || (vars[0] + vars[1] + vars[2]).let { it.isEmpty() || !POLAR_XY.containsAll(it) }) {
                        throw unsupported()
                    }
                    val g1 = field(ops[0], r1, ops[1])
                    val g2 = field(ops[1], r2, ops[2])
                    val f = fn(MathNode.Call("max", listOf(g1, g2)), "x", "y", polar = true)
                    CompiledLine(id, GraphLineKind.REGION, latex, plot = GraphPlot.Region(f, r1.isStrict && r2.isStrict))
                }
                else -> throw unsupported()
            }
        }

        private fun unsupported(kind: GraphErrorKind = GraphErrorKind.UNSUPPORTED_EQUATION) = CompileException(GraphError(kind))

        private fun bare(id: Long, latex: String, node: MathNode, vars: Set<String>): CompiledLine {
            if (node is MathNode.Tuple) {
                if (node.items.size != 2) throw unsupported()
                return when {
                    vars.isEmpty() -> {
                        val x = fn(node.items[0]).evaluate()
                        val y = fn(node.items[1]).evaluate()
                        CompiledLine(id, GraphLineKind.POINT, latex, plot = GraphPlot.Point(x, y))
                    }
                    vars == setOf("t") -> CompiledLine(
                        id, GraphLineKind.PARAMETRIC, latex,
                        plot = GraphPlot.Parametric(fn(node.items[0], "t"), fn(node.items[1], "t"), fullTurn()),
                    )
                    else -> throw unsupported()
                }
            }
            if (node is MathNode.Integral && vars.isEmpty()) integralArea(id, latex, node)?.let { return it }
            return when {
                vars.isEmpty() -> CompiledLine(id, GraphLineKind.VALUE, latex, value = fn(node).evaluate())
                vars == setOf("x") -> CompiledLine(id, GraphLineKind.EXPLICIT, latex, plot = explicit(node))
                else -> throw unsupported()
            }
        }

        /** ∫_a^b f dx with constant bounds: shade it. f − g shades between the two curves. */
        private fun integralArea(id: Long, latex: String, node: MathNode.Integral): CompiledLine? {
            val v = node.variable
            val bodyVars = MathTree.freeSymbols(node.body).filter { it in RESERVED_VARIABLES && it != v }
            if (bodyVars.isNotEmpty()) return null
            val lower = fn(node.lower).evaluate()
            val upper = fn(node.upper).evaluate()
            val body = node.body
            val between = body is MathNode.Bin && body.op == BinOp.SUB &&
                MathTree.dependsOn(body.left, v) && MathTree.dependsOn(body.right, v)
            val top = fn(if (between) (body as MathNode.Bin).left else body, v)
            val bottom = if (between) fn((body as MathNode.Bin).right, v) else null
            val integrand = fn(body, v)
            val env = integrand.newEnv()
            val result = Integrator.integrate(integrand.asFunctionOf(0, env), lower, upper)
            return CompiledLine(
                id, GraphLineKind.INTEGRAL, latex,
                value = result.value,
                plot = GraphPlot.IntegralArea(top, bottom, lower, upper, result),
            )
        }

        private fun relation(
            id: Long,
            latex: String,
            left: MathNode,
            rel: Relation,
            right: MathNode,
            leftVars: Set<String>,
            rightVars: Set<String>,
        ): CompiledLine {
            val all = leftVars + rightVars
            if ("t" in all || all.isEmpty()) throw unsupported()
            fun isSym(n: MathNode, name: String) = n is MathNode.Sym && n.name == name

            if (!rel.isInequality) {
                return when {
                    isSym(left, "y") && rightVars.all { it == "x" } -> CompiledLine(id, GraphLineKind.EXPLICIT, latex, plot = explicit(right))
                    isSym(right, "y") && leftVars.all { it == "x" } -> CompiledLine(id, GraphLineKind.EXPLICIT, latex, plot = explicit(left))
                    isSym(left, "x") && rightVars.all { it == "y" } -> CompiledLine(id, GraphLineKind.EXPLICIT, latex, plot = GraphPlot.ExplicitX(fn(right, "y")))
                    isSym(right, "x") && leftVars.all { it == "y" } -> CompiledLine(id, GraphLineKind.EXPLICIT, latex, plot = GraphPlot.ExplicitX(fn(left, "y")))
                    isSym(left, "r") && rightVars.all { it == "θ" } -> polar(id, latex, right)
                    isSym(right, "r") && leftVars.all { it == "θ" } -> polar(id, latex, left)
                    POLAR_XY.containsAll(all) -> CompiledLine(
                        id, GraphLineKind.IMPLICIT, latex,
                        plot = GraphPlot.Implicit(fn(MathNode.Bin(BinOp.SUB, left, right), "x", "y", polar = true)),
                    )
                    else -> throw unsupported()
                }
            }
            return when {
                isSym(left, "y") && rightVars.all { it == "x" } ->
                    CompiledLine(id, GraphLineKind.INEQUALITY, latex, plot = GraphPlot.ExplicitYInequality(fn(right, "x"), rel))
                isSym(right, "y") && leftVars.all { it == "x" } ->
                    CompiledLine(id, GraphLineKind.INEQUALITY, latex, plot = GraphPlot.ExplicitYInequality(fn(left, "x"), rel.flipped()))
                isSym(left, "x") && rightVars.all { it == "y" } ->
                    CompiledLine(id, GraphLineKind.INEQUALITY, latex, plot = GraphPlot.ExplicitXInequality(fn(right, "y"), rel))
                isSym(right, "x") && leftVars.all { it == "y" } ->
                    CompiledLine(id, GraphLineKind.INEQUALITY, latex, plot = GraphPlot.ExplicitXInequality(fn(left, "y"), rel.flipped()))
                POLAR_XY.containsAll(all) -> CompiledLine(
                    id, GraphLineKind.REGION, latex,
                    plot = GraphPlot.Region(fn(field(left, rel, right), "x", "y", polar = true), rel.isStrict),
                )
                else -> throw unsupported()
            }
        }

        /** A field that is negative exactly where `left rel right` holds. */
        private fun field(left: MathNode, rel: Relation, right: MathNode): MathNode = when (rel) {
            Relation.LT, Relation.LE -> MathNode.Bin(BinOp.SUB, left, right)
            Relation.GT, Relation.GE -> MathNode.Bin(BinOp.SUB, right, left)
            Relation.EQ -> throw unsupported()
        }

        private fun polar(id: Long, latex: String, body: MathNode): CompiledLine {
            val toRadians = if (angleUnit == AngleUnit.DEG) PI / 180 else 1.0
            return CompiledLine(id, GraphLineKind.POLAR, latex, plot = GraphPlot.Polar(fn(body, "θ"), toRadians, fullTurn()))
        }

        private fun fullTurn(): ClosedFloatingPointRange<Double> = if (angleUnit == AngleUnit.DEG) 0.0..360.0 else 0.0..(2 * PI)
    }

    companion object {
        /** Variables a graph line may use; they can't be assigned (x = 3 is a vertical line, not a parameter). */
        val RESERVED_VARIABLES = setOf("x", "y", "z", "t", "θ", "r")
        private val POLAR_XY = setOf("x", "y", "r", "θ")

        val BUILTIN_CONSTANTS = mapOf(
            "π" to PI, "τ" to 2 * PI, "e" to E, "∞" to Double.POSITIVE_INFINITY,
        )

        private val DEFINITION = Regex("""^\s*([A-Za-zα-ωΑ-Ω](?:_[A-Za-z0-9]+|_\{[A-Za-z0-9]+\})?)\s*\(([^()=]*)\)\s*=""")

        /** `f(x) = …` → "f" (`a_{1}(x)` → "a_1"); null for anything else. */
        fun definedFunctionName(text: String): String? {
            val match = DEFINITION.find(text) ?: return null
            val name = match.groupValues[1].replace("{", "").replace("}", "")
            if (name in RESERVED_VARIABLES || name in BUILTIN_CONSTANTS || MathFunctions.isBuiltin(name)) return null
            if (match.groupValues[2].isBlank()) return null
            return name
        }
    }
}
