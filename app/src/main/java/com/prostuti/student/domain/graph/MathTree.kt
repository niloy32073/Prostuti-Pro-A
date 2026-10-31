package com.prostuti.student.domain.graph

/** Tree queries and rewrites that respect binders (∫ … dt, Σ_n, Let). */
internal object MathTree {

    /** Names used but not bound inside [node]. */
    fun freeSymbols(node: MathNode): Set<String> {
        val out = LinkedHashSet<String>()
        collect(node, emptySet(), out)
        return out
    }

    fun dependsOn(node: MathNode, name: String): Boolean = name in freeSymbols(node)

    private fun collect(node: MathNode, bound: Set<String>, out: MutableSet<String>) {
        when (node) {
            is MathNode.Num -> Unit
            is MathNode.Sym -> if (node.name !in bound) out += node.name
            is MathNode.Neg -> collect(node.operand, bound, out)
            is MathNode.Bin -> { collect(node.left, bound, out); collect(node.right, bound, out) }
            is MathNode.Call -> node.args.forEach { collect(it, bound, out) }
            is MathNode.Diff -> { if (node.variable !in bound) out += node.variable; collect(node.body, bound, out) }
            is MathNode.NumericDiff -> { if (node.variable !in bound) out += node.variable; collect(node.body, bound, out) }
            is MathNode.Integral -> {
                collect(node.lower, bound, out)
                collect(node.upper, bound, out)
                collect(node.body, bound + node.variable, out)
            }
            is MathNode.Series -> {
                collect(node.from, bound, out)
                collect(node.to, bound, out)
                collect(node.body, bound + node.variable, out)
            }
            is MathNode.Piecewise -> {
                node.branches.forEach { b ->
                    b.condition.operands.forEach { collect(it, bound, out) }
                    collect(b.value, bound, out)
                }
                node.otherwise?.let { collect(it, bound, out) }
            }
            is MathNode.Tuple -> node.items.forEach { collect(it, bound, out) }
            is MathNode.Let -> { collect(node.value, bound, out); collect(node.body, bound + node.name, out) }
        }
    }

    /** User-function names called anywhere in [node]. */
    fun calledFunctions(node: MathNode): Set<String> {
        val out = LinkedHashSet<String>()
        visit(node) { if (it is MathNode.Call && !MathFunctions.isBuiltin(it.name)) out += it.name }
        return out
    }

    fun containsTuple(node: MathNode): Boolean {
        var found = false
        visit(node) { if (it is MathNode.Tuple) found = true }
        return found
    }

    fun visit(node: MathNode, action: (MathNode) -> Unit) {
        action(node)
        when (node) {
            is MathNode.Num, is MathNode.Sym -> Unit
            is MathNode.Neg -> visit(node.operand, action)
            is MathNode.Bin -> { visit(node.left, action); visit(node.right, action) }
            is MathNode.Call -> node.args.forEach { visit(it, action) }
            is MathNode.Diff -> visit(node.body, action)
            is MathNode.NumericDiff -> visit(node.body, action)
            is MathNode.Integral -> { visit(node.lower, action); visit(node.upper, action); visit(node.body, action) }
            is MathNode.Series -> { visit(node.from, action); visit(node.to, action); visit(node.body, action) }
            is MathNode.Piecewise -> {
                node.branches.forEach { b -> b.condition.operands.forEach { visit(it, action) }; visit(b.value, action) }
                node.otherwise?.let { visit(it, action) }
            }
            is MathNode.Tuple -> node.items.forEach { visit(it, action) }
            is MathNode.Let -> { visit(node.value, action); visit(node.body, action) }
        }
    }

    /** Renames free occurrences of [from] to [to]. [to] must be a fresh name (nothing binds it). */
    fun renameFree(node: MathNode, from: String, to: String): MathNode {
        fun r(n: MathNode) = renameFree(n, from, to)
        return when (node) {
            is MathNode.Num -> node
            is MathNode.Sym -> if (node.name == from) MathNode.Sym(to) else node
            is MathNode.Neg -> MathNode.Neg(r(node.operand))
            is MathNode.Bin -> MathNode.Bin(node.op, r(node.left), r(node.right))
            is MathNode.Call -> MathNode.Call(node.name, node.args.map(::r), node.primes)
            is MathNode.Diff -> MathNode.Diff(if (node.variable == from) to else node.variable, r(node.body))
            is MathNode.NumericDiff -> MathNode.NumericDiff(if (node.variable == from) to else node.variable, r(node.body))
            is MathNode.Integral -> MathNode.Integral(
                node.variable, r(node.lower), r(node.upper),
                if (node.variable == from) node.body else r(node.body),
            )
            is MathNode.Series -> MathNode.Series(
                node.variable, r(node.from), r(node.to),
                if (node.variable == from) node.body else r(node.body), node.product,
            )
            is MathNode.Piecewise -> MathNode.Piecewise(
                node.branches.map { b ->
                    MathNode.Branch(MathNode.Condition(b.condition.operands.map(::r), b.condition.relations), r(b.value))
                },
                node.otherwise?.let(::r),
            )
            is MathNode.Tuple -> MathNode.Tuple(node.items.map(::r))
            is MathNode.Let -> MathNode.Let(node.name, r(node.value), if (node.name == from) node.body else r(node.body))
        }
    }

    /** Bottom-up rewrite; [transform] sees each node after its children were rewritten. */
    fun rewrite(node: MathNode, transform: (MathNode) -> MathNode): MathNode {
        fun r(n: MathNode) = rewrite(n, transform)
        val rebuilt = when (node) {
            is MathNode.Num, is MathNode.Sym -> node
            is MathNode.Neg -> MathNode.Neg(r(node.operand))
            is MathNode.Bin -> MathNode.Bin(node.op, r(node.left), r(node.right))
            is MathNode.Call -> MathNode.Call(node.name, node.args.map(::r), node.primes)
            is MathNode.Diff -> MathNode.Diff(node.variable, r(node.body))
            is MathNode.NumericDiff -> MathNode.NumericDiff(node.variable, r(node.body))
            is MathNode.Integral -> MathNode.Integral(node.variable, r(node.lower), r(node.upper), r(node.body))
            is MathNode.Series -> MathNode.Series(node.variable, r(node.from), r(node.to), r(node.body), node.product)
            is MathNode.Piecewise -> MathNode.Piecewise(
                node.branches.map { b ->
                    MathNode.Branch(MathNode.Condition(b.condition.operands.map(::r), b.condition.relations), r(b.value))
                },
                node.otherwise?.let(::r),
            )
            is MathNode.Tuple -> MathNode.Tuple(node.items.map(::r))
            is MathNode.Let -> MathNode.Let(node.name, r(node.value), r(node.body))
        }
        return transform(rebuilt)
    }
}
