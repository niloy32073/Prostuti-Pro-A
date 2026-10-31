package com.prostuti.student.domain.graph

/**
 * Light algebraic clean-up after differentiation: folds numbers and drops 0/1 identities so
 * derivative trees stay small (and fast). Not a CAS — it never reorders or factors terms.
 */
internal object Simplifier {

    fun simplify(node: MathNode): MathNode = MathTree.rewrite(node, ::step)

    private fun num(node: MathNode): Double? = (node as? MathNode.Num)?.value

    private fun step(node: MathNode): MathNode = when (node) {
        is MathNode.Neg -> when (val o = node.operand) {
            is MathNode.Num -> MathNode.Num(-o.value)
            is MathNode.Neg -> o.operand
            else -> node
        }
        is MathNode.Bin -> bin(node)
        is MathNode.Let -> if (MathTree.dependsOn(node.body, node.name)) node else node.body
        else -> node
    }

    private fun bin(node: MathNode.Bin): MathNode {
        val a = node.left
        val b = node.right
        val x = num(a)
        val y = num(b)
        return when (node.op) {
            BinOp.ADD -> when {
                x != null && y != null -> MathNode.Num(x + y)
                x == 0.0 -> b
                y == 0.0 -> a
                b is MathNode.Neg -> MathNode.Bin(BinOp.SUB, a, b.operand)
                else -> node
            }
            BinOp.SUB -> when {
                x != null && y != null -> MathNode.Num(x - y)
                y == 0.0 -> a
                x == 0.0 -> MathNode.Neg(b)
                b is MathNode.Neg -> MathNode.Bin(BinOp.ADD, a, b.operand)
                a == b -> MathNode.Num(0.0)
                else -> node
            }
            BinOp.MUL -> when {
                x != null && y != null -> MathNode.Num(x * y)
                x == 0.0 || y == 0.0 -> MathNode.Num(0.0)
                x == 1.0 -> b
                y == 1.0 -> a
                x == -1.0 -> MathNode.Neg(b)
                y == -1.0 -> MathNode.Neg(a)
                x != null && b is MathNode.Bin && b.op == BinOp.MUL && b.left is MathNode.Num ->
                    MathNode.Bin(BinOp.MUL, MathNode.Num(x * (b.left as MathNode.Num).value), b.right)
                y != null -> MathNode.Bin(BinOp.MUL, b, a) // numbers first: x·3 → 3x
                else -> node
            }
            BinOp.DIV -> when {
                x != null && y != null && y != 0.0 -> MathNode.Num(x / y)
                y == 1.0 -> a
                x == 0.0 -> MathNode.Num(0.0)
                else -> node
            }
            BinOp.POW -> when {
                x != null && y != null -> MathNode.Num(SpecialFunctions.realPow(x, y))
                y == 1.0 -> a
                y == 0.0 -> MathNode.Num(1.0)
                x == 1.0 -> MathNode.Num(1.0)
                else -> node
            }
        }
    }
}
