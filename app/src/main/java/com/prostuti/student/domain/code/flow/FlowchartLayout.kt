package com.prostuti.student.domain.code.flow

import kotlin.math.max
import kotlin.math.min

/**
 * Lays out one [FlowFunction] top-down, the textbook way: the main flow runs down a vertical axis,
 * a decision's "yes" goes down and its "no" goes right, a loop's back edge returns up on the left
 * and its exit leaves down on the right. Symbols never overlap.
 *
 * `break`, `continue`, `return` and `raise` lines leave their symbol on the right and run down
 * lanes to the right of everything below them, until the loop (or the function's end) they belong
 * to picks them up. Lines may cross other lines, never symbols.
 */
class FlowchartLayout(private val metrics: FlowMetrics, private val textWidth: TextWidth) {

    private val lh = metrics.lineHeight
    private val cw = metrics.charWidth
    private val padX = cw * 1.4f
    private val padY = lh * 0.45f
    private val vGap = lh * 1.3f
    private val hGap = cw * 3f
    private val lane = cw * 1.6f
    private val jog = lh * 0.45f
    private val margin = cw * 5f

    /** A jump's line that is not finished yet; its last point is on the bottom edge of its box. */
    private class Pending(val kind: JumpKind, val path: MutableList<FlowPoint>) {
        val x: Float get() = path.last().x
        val y: Float get() = path.last().y
        fun down(toY: Float) { if (toY > y) path += FlowPoint(x, toY) }
        fun to(px: Float, py: Float) { path += FlowPoint(px, py) }
    }

    /** A laid-out piece. Its flow enters at (0, 0) and, when [hasExit], leaves at (0, [height]). */
    private class Box {
        var left = 0f
        var right = 0f
        var height = 0f
        var hasExit = true
        /** The entry is a symbol's top (gets an arrowhead), not a line that runs on. */
        var entryIsNode = false
        val nodes = mutableListOf<FlowNode>()
        val edges = mutableListOf<FlowEdge>()
        val pendings = mutableListOf<Pending>()
        val isEmpty: Boolean get() = nodes.isEmpty()
    }

    fun layout(function: FlowFunction): Flowchart {
        val out = Box()
        val start = symbol(NodeShape.TERMINAL, if (function.isEntry) metrics.startLabel else function.header, function.line)
        out.nodes += start
        val body = seq(function.body)
        val bodyTop = start.height + vGap
        out.line(pt(0f, start.height), pt(0f, bodyTop), arrow = body.entryIsNode)
        val pend = body.placeInto(out, 0f, bodyTop)
        val bodyBottom = bodyTop + body.height
        val end = symbol(NodeShape.TERMINAL, metrics.endLabel, function.line)
        reroute(pend, bodyBottom, end.width / 2 + hGap)
        val endTop = bodyBottom + vGap + pend.size * jog
        if (body.hasExit) out.line(pt(0f, bodyBottom), pt(0f, endTop), arrow = true)
        out.nodes += end.copy(y = endTop)
        val endMid = endTop + end.height / 2
        pend.forEach { it.down(endMid); it.to(end.width / 2, endMid); out.edge(it.path, arrow = true) }
        return finish(out)
    }

    // ── Pieces ──

    private fun block(b: FlowBlock): Box = when (b) {
        is FlowBlock.Step -> nodeBox(stepSymbol(b))
        is FlowBlock.Jump -> jumpBox(b)
        is FlowBlock.Branch -> branch(b)
        is FlowBlock.Loop -> if (b.kind == LoopKind.DO_WHILE) doWhile(b) else whileLoop(b)
        is FlowBlock.Switch -> switch(b)
        is FlowBlock.Connector -> nodeBox(symbol(NodeShape.CONNECTOR, b.label, b.line)).apply { hasExit = !b.isGoto }
    }

    private fun seq(blocks: List<FlowBlock>): Box {
        val boxes = blocks.map { block(it) }
        val out = Box()
        if (boxes.isEmpty()) return out
        val rightBelow = FloatArray(boxes.size)
        for (i in boxes.indices.reversed()) rightBelow[i] = max(boxes[i].right, if (i + 1 < boxes.size) rightBelow[i + 1] else 0f)
        out.entryIsNode = boxes[0].entryIsNode
        var y = 0f
        var open = true
        boxes.forEachIndexed { i, b ->
            if (i > 0) {
                val top = y + vGap + out.pendings.size * jog
                reroute(out.pendings, y, rightBelow[i] + hGap)
                if (open) out.line(pt(0f, y), pt(0f, top), arrow = b.entryIsNode)
                y = top
            }
            val placed = b.placeInto(out, 0f, y)
            y += b.height
            out.pendings.forEach { it.down(y) }
            out.pendings += placed
            open = b.hasExit
            out.left = min(out.left, b.left)
            out.right = max(out.right, b.right)
        }
        out.height = y
        out.hasExit = open
        out.right = max(out.right, out.pendings.maxOfOrNull { it.x } ?: 0f)
        return out
    }

    private fun nodeBox(n: FlowNode): Box = Box().apply {
        nodes += n
        left = -n.width / 2
        right = n.width / 2
        height = n.height
        entryIsNode = true
    }

    private fun jumpBox(j: FlowBlock.Jump): Box {
        val n = symbol(if (j.callee != null) NodeShape.CALL else NodeShape.PROCESS, j.text, j.line, j.callee)
        val box = nodeBox(n)
        val out = n.width / 2 + lane
        val mid = n.height / 2
        box.hasExit = false
        box.height = n.height + jog
        box.right = out
        box.pendings += Pending(j.kind, mutableListOf(pt(n.width / 2, mid), pt(out, mid), pt(out, box.height)))
        return box
    }

    private fun branch(b: FlowBlock.Branch): Box {
        val out = Box().apply { entryIsNode = true }
        val d = symbol(NodeShape.DECISION, b.condition, b.line)
        out.nodes += d
        val dw2 = d.width / 2
        val dMid = d.height / 2
        val dBottom = d.height
        val yes = seq(b.yes)
        val no = seq(b.no)
        val top = dBottom + vGap
        val yesPend = yes.placeInto(out, 0f, top)
        val yesBottom = top + yes.height
        val noAxis = max(dw2, yes.right) + hGap + if (no.isEmpty) 0f else -no.left
        val noPend = no.placeInto(out, noAxis, top)
        val noBottom = top + no.height
        val mergeY = max(yesBottom, noBottom) + vGap * 0.8f
        val yesLabel = pt(cw * 0.5f, dBottom + lh * 0.1f)
        val noLabel = pt(dw2 + cw * 0.6f, dMid - lh * 1.1f)
        val yesReaches = yes.isEmpty || yes.hasExit
        val noReaches = no.isEmpty || no.hasExit
        if (yes.isEmpty) {
            out.line(pt(0f, dBottom), pt(0f, mergeY), arrow = false, label = EdgeLabel.YES, labelAt = yesLabel)
        } else {
            out.line(pt(0f, dBottom), pt(0f, top), arrow = yes.entryIsNode, label = EdgeLabel.YES, labelAt = yesLabel)
            if (yes.hasExit) out.line(pt(0f, yesBottom), pt(0f, mergeY), arrow = false)
        }
        if (no.isEmpty) {
            out.line(pt(dw2, dMid), pt(noAxis, dMid), pt(noAxis, mergeY), pt(0f, mergeY), arrow = yesReaches, label = EdgeLabel.NO, labelAt = noLabel)
        } else {
            out.line(pt(dw2, dMid), pt(noAxis, dMid), pt(noAxis, top), arrow = no.entryIsNode, label = EdgeLabel.NO, labelAt = noLabel)
            if (no.hasExit) out.line(pt(noAxis, noBottom), pt(noAxis, mergeY), pt(0f, mergeY), arrow = yesReaches)
        }
        out.hasExit = yesReaches || noReaches
        val bottom = mergeY + jog
        if (out.hasExit) out.line(pt(0f, mergeY), pt(0f, bottom), arrow = false)
        (yesPend + noPend).forEach { it.down(bottom); out.pendings += it }
        out.left = min(-dw2, yes.left)
        out.right = maxOf(dw2, noAxis + if (no.isEmpty) 0f else no.right, out.pendings.maxOfOrNull { it.x } ?: 0f)
        out.height = bottom
        return out
    }

    /** `while`, `for`: [init] → condition → body → [update] → back up to the condition. */
    private fun whileLoop(l: FlowBlock.Loop): Box {
        val out = Box()
        val init = l.init?.let { stepSymbol(it) }
        var top = 0f
        if (init != null) { out.nodes += init; top = init.height; out.entryIsNode = true }
        val joinY = top + vGap * 0.5f
        val d = symbol(NodeShape.DECISION, l.condition, l.line)
        val dTop = joinY + vGap * 0.7f
        out.nodes += d.copy(y = dTop)
        out.line(pt(0f, top), pt(0f, dTop), arrow = true)
        val dw2 = d.width / 2
        val dMid = dTop + d.height / 2
        val dBottom = dTop + d.height
        val body = seq(l.body)
        val bodyTop = dBottom + vGap
        out.line(pt(0f, dBottom), pt(0f, bodyTop), arrow = body.entryIsNode, label = EdgeLabel.YES, labelAt = pt(cw * 0.5f, dBottom + lh * 0.1f))
        val pend = body.placeInto(out, 0f, bodyTop)
        val bodyBottom = bodyTop + body.height
        val breaks = pend.filter { it.kind == JumpKind.BREAK }.sortedByDescending { it.x }
        val continues = pend.filter { it.kind == JumpKind.CONTINUE }.sortedBy { it.x }
        val passing = pend.filter { it.kind == JumpKind.RETURN || it.kind == JumpKind.RAISE }
        var y = bodyBottom
        val breakYs = breaks.map { y += jog; it.down(y); y }
        val continueYs = continues.map { y += jog; it.down(y); it.to(0f, y); out.edge(it.path, arrow = true); y }
        val update = l.update?.let { stepSymbol(it) }
        reroute(passing, y, (update?.width ?: 0f) / 2 + hGap)
        y += passing.size * jog
        var leftLane = minOf(-dw2, body.left, -(init?.width ?: 0f) / 2, -(update?.width ?: 0f) / 2) - hGap
        var backY = y
        if (body.hasExit || continues.isNotEmpty()) {
            val latchStart = if (body.hasExit) bodyBottom else continueYs.first()
            val latchY = y + vGap * 0.4f
            var from = latchStart
            if (update != null) {
                out.nodes += update.copy(y = latchY)
                out.line(pt(0f, latchStart), pt(0f, latchY), arrow = true)
                from = latchY + update.height
            }
            backY = max(from, latchY) + vGap * 0.6f
            out.line(pt(0f, from), pt(0f, backY), pt(leftLane, backY), pt(leftLane, joinY), pt(0f, joinY), arrow = true)
        } else {
            leftLane = min(-dw2, body.left)
        }
        val xE = maxOf(dw2, body.right, (update?.width ?: 0f) / 2, (init?.width ?: 0f) / 2, passing.maxOfOrNull { it.x } ?: 0f) + hGap
        val exitY = backY + vGap * 0.7f
        out.line(pt(dw2, dMid), pt(xE, dMid), pt(xE, exitY), pt(0f, exitY), arrow = false, label = EdgeLabel.NO, labelAt = pt(dw2 + cw * 0.6f, dMid - lh * 1.1f))
        val bottom: Float
        if (l.orElse.isEmpty()) {
            breaks.forEachIndexed { k, b -> b.to(xE, breakYs[k]); out.edge(b.path, arrow = true) }
            bottom = exitY + jog
            out.line(pt(0f, exitY), pt(0f, bottom), arrow = false)
            passing.forEach { it.down(bottom) }
            out.pendings += passing
            out.right = xE
        } else {
            // Python `while … else`: "no" runs the else block; `break` skips it.
            val e = seq(l.orElse)
            reroute(passing, exitY, e.right + hGap)
            val eTop = exitY + vGap + passing.size * jog
            out.line(pt(0f, exitY), pt(0f, eTop), arrow = e.entryIsNode)
            val ePend = e.placeInto(out, 0f, eTop)
            val eBottom = eTop + e.height
            val mergeY = eBottom + vGap * 0.8f
            if (e.hasExit) out.line(pt(0f, eBottom), pt(0f, mergeY), arrow = false)
            val xB = maxOf(xE, e.right, passing.maxOfOrNull { it.x } ?: 0f, ePend.maxOfOrNull { it.x } ?: 0f) + lane
            breaks.forEachIndexed { k, b -> b.to(xB, breakYs[k]); b.to(xB, mergeY); b.to(0f, mergeY); out.edge(b.path, arrow = true) }
            bottom = mergeY + jog
            out.line(pt(0f, mergeY), pt(0f, bottom), arrow = false)
            (passing + ePend).forEach { it.down(bottom); out.pendings += it }
            out.right = if (breaks.isEmpty()) max(xE, e.right) else xB
        }
        out.left = leftLane
        out.height = bottom
        out.hasExit = true
        return out
    }

    /** `do { body } while (condition);` */
    private fun doWhile(l: FlowBlock.Loop): Box {
        val out = Box()
        val joinY = vGap * 0.5f
        val body = seq(l.body)
        val bodyTop = joinY + vGap * 0.7f
        out.line(pt(0f, 0f), pt(0f, bodyTop), arrow = body.entryIsNode)
        val pend = body.placeInto(out, 0f, bodyTop)
        val bodyBottom = bodyTop + body.height
        val breaks = pend.filter { it.kind == JumpKind.BREAK }.sortedByDescending { it.x }
        val continues = pend.filter { it.kind == JumpKind.CONTINUE }.sortedBy { it.x }
        val passing = pend.filter { it.kind == JumpKind.RETURN || it.kind == JumpKind.RAISE }
        var y = bodyBottom
        val breakYs = breaks.map { y += jog; it.down(y); y }
        val continueYs = continues.map { y += jog; it.down(y); it.to(0f, y); out.edge(it.path, arrow = true); y }
        val d = symbol(NodeShape.DECISION, l.condition, l.line)
        val dw2 = d.width / 2
        reroute(passing, y, dw2 + hGap)
        y += passing.size * jog
        val dTop = y + vGap * 0.5f
        if (body.hasExit || continues.isNotEmpty()) {
            out.line(pt(0f, if (body.hasExit) bodyBottom else continueYs.first()), pt(0f, dTop), arrow = true)
        }
        out.nodes += d.copy(y = dTop)
        val dMid = dTop + d.height / 2
        val dBottom = dTop + d.height
        val leftLane = min(-dw2, body.left) - hGap
        out.line(
            pt(-dw2, dMid), pt(leftLane, dMid), pt(leftLane, joinY), pt(0f, joinY), arrow = true,
            label = EdgeLabel.YES, labelAt = pt(-dw2 - cw * 0.6f, dMid - lh * 1.1f), labelAlignEnd = true,
        )
        val exitY = dBottom + vGap
        out.line(pt(0f, dBottom), pt(0f, exitY), arrow = false, label = EdgeLabel.NO, labelAt = pt(cw * 0.5f, dBottom + lh * 0.1f))
        val xE = maxOf(dw2, body.right, passing.maxOfOrNull { it.x } ?: 0f) + hGap
        breaks.forEachIndexed { k, b -> b.to(xE, breakYs[k]); b.to(xE, exitY); b.to(0f, exitY); out.edge(b.path, arrow = true) }
        val bottom = exitY + jog
        out.line(pt(0f, exitY), pt(0f, bottom), arrow = false)
        passing.forEach { it.down(bottom) }
        out.pendings += passing
        out.left = leftLane
        out.right = if (breaks.isEmpty()) xE - hGap else xE
        out.height = bottom
        return out
    }

    /**
     * Diamonds down the axis (one per case), case bodies in a column to their right. A C switch
     * falls from one body into the next; `break` (and a match case's end) leaves on an exit lane.
     */
    private fun switch(s: FlowBlock.Switch): Box {
        val ordered = s.cases.filter { it.test != null } + s.cases.filter { it.test == null }.take(1)
        if (ordered.isEmpty()) return Box()
        val out = Box()
        val diamonds = ordered.filter { it.test != null }.map { symbol(NodeShape.DECISION, it.test!!, it.line) }
        val hasDefault = ordered.last().test == null
        val bodies = ordered.map { seq(it.body) }
        val maxD2 = diamonds.maxOfOrNull { it.width / 2 } ?: 0f
        val colLeft = bodies.minOf { it.left }
        val xB = if (diamonds.isEmpty()) 0f else maxD2 + hGap - colLeft
        val xE = max(maxD2, xB + bodies.maxOf { it.right }) + hGap
        out.entryIsNode = diamonds.isNotEmpty() || bodies[0].entryIsNode
        val exitJoins = mutableListOf<Pending>()
        val passing = mutableListOf<Pending>()
        var passLimit = xE + lane
        var prevDBottom = 0f
        var prevBody: Box? = null
        var prevBodyBottom = 0f
        var nextFree = 0f
        ordered.forEachIndexed { i, case ->
            val body = bodies[i]
            val bodyTop: Float
            if (case.test != null) {
                val d = diamonds[i]
                val rowTop = if (i == 0) 0f else max(prevDBottom + vGap * 0.8f, nextFree - d.height / 2)
                if (i > 0) out.line(pt(0f, prevDBottom), pt(0f, rowTop), arrow = true, label = EdgeLabel.NO, labelAt = pt(cw * 0.5f, prevDBottom + lh * 0.1f))
                out.nodes += d.copy(y = rowTop)
                val dMid = rowTop + d.height / 2
                bodyTop = dMid + vGap * 0.6f
                out.line(
                    pt(d.width / 2, dMid), pt(xB, dMid), pt(xB, bodyTop), arrow = body.entryIsNode,
                    label = EdgeLabel.YES, labelAt = pt(d.width / 2 + cw * 0.6f, dMid - lh * 1.1f),
                )
                if (s.fallsThrough && prevBody?.hasExit == true) out.line(pt(xB, prevBodyBottom), pt(xB, dMid), arrow = false)
                prevDBottom = rowTop + d.height
            } else if (diamonds.isEmpty()) {
                bodyTop = 0f
            } else {
                val joinY = max(prevDBottom + vGap * 0.6f, nextFree)
                bodyTop = joinY + vGap * 0.5f
                out.line(
                    pt(0f, prevDBottom), pt(0f, joinY), pt(xB, joinY), pt(xB, bodyTop), arrow = body.entryIsNode,
                    label = EdgeLabel.NO, labelAt = pt(cw * 0.5f, prevDBottom + lh * 0.1f),
                )
                if (s.fallsThrough && prevBody?.hasExit == true) out.line(pt(xB, prevBodyBottom), pt(xB, joinY), arrow = false)
            }
            val pend = body.placeInto(out, xB, bodyTop)
            val bodyBottom = bodyTop + body.height
            var y = bodyBottom
            val (breaks, rest) = pend.partition { s.fallsThrough && it.kind == JumpKind.BREAK }
            breaks.sortedByDescending { it.x }.forEach { y += jog; it.down(y); exitJoins += it }
            val last = i == ordered.lastIndex
            if (body.hasExit && (!s.fallsThrough || last)) {
                y += jog
                exitJoins += Pending(JumpKind.BREAK, mutableListOf(pt(xB, bodyBottom), pt(xB, y)))
            }
            val sorted = rest.sortedBy { it.x }
            val targets = sorted.map { p -> max(p.x, passLimit).also { passLimit = it + lane } }
            for (k in sorted.indices.reversed()) { y += jog; sorted[k].down(y); sorted[k].to(targets[k], y) }
            passing += sorted
            nextFree = y + vGap * 0.6f
            prevBody = body
            prevBodyBottom = bodyBottom
        }
        val exitY = max(nextFree, prevDBottom + vGap)
        val noMatch = !hasDefault && diamonds.isNotEmpty()
        if (noMatch) out.line(pt(0f, prevDBottom), pt(0f, exitY), arrow = false, label = EdgeLabel.NO, labelAt = pt(cw * 0.5f, prevDBottom + lh * 0.1f))
        if (exitJoins.isNotEmpty()) {
            exitJoins.forEach { it.to(xE, it.y); out.edge(it.path, arrow = true) }
            out.line(pt(xE, exitJoins.minOf { it.y }), pt(xE, exitY), pt(0f, exitY), arrow = noMatch)
        }
        out.hasExit = exitJoins.isNotEmpty() || noMatch
        val bottom = exitY + jog
        if (out.hasExit) out.line(pt(0f, exitY), pt(0f, bottom), arrow = false)
        passing.forEach { it.down(bottom); out.pendings += it }
        out.left = min(-maxD2, xB + colLeft)
        out.right = max(xE, passing.maxOfOrNull { it.x } ?: 0f)
        out.height = bottom
        return out
    }

    // ── Helpers ──

    private fun stepSymbol(s: FlowBlock.Step): FlowNode = symbol(
        when (s.kind) {
            StepKind.PROCESS -> NodeShape.PROCESS
            StepKind.IO -> NodeShape.IO
            StepKind.CALL -> NodeShape.CALL
        },
        s.text, s.line, s.callee,
    )

    /** A symbol sized to its wrapped text, centred on x = 0 with its top at y = 0. */
    private fun symbol(shape: NodeShape, text: String, line: Int, callee: String? = null): FlowNode {
        val maxChars = if (shape == NodeShape.DECISION) metrics.maxDecisionChars else metrics.maxChars
        val lines = TextWrap.wrap(text, maxChars, metrics.maxLines)
        val tw = lines.maxOf { textWidth.of(it) }
        val th = lines.size * lh
        var w: Float
        val h: Float
        when (shape) {
            NodeShape.PROCESS -> { w = tw + 2 * padX; h = th + 2 * padY }
            NodeShape.CALL -> { w = tw + 2 * padX + 2 * cw * CALL_BAR_CHARS; h = th + 2 * padY }
            NodeShape.IO -> { h = th + 2 * padY; w = tw + 2 * padX + 2 * h * IO_SKEW }
            NodeShape.TERMINAL -> { h = th + 2 * padY; w = tw + 2 * padX + h * 0.6f }
            NodeShape.CONNECTOR -> {
                val d = max(max(tw, th) + 2 * padY, lh * 1.8f)
                return FlowNode(0, shape, lines, line, 0f, 0f, d, d, callee)
            }
            NodeShape.DECISION -> {
                // The text box (tw × th) must fit inside the rhombus: (tw/2)/a + (th/2)/b ≤ 1.
                val a = maxOf(tw / 2 * 1.6f, tw / 2 + cw * 2, lh * 1.8f)
                val b = max((th / 2) / (1 - (tw / 2) / a), th / 2 + padY)
                w = 2 * a
                h = 2 * b
            }
        }
        w = max(w, cw * 8)
        return FlowNode(0, shape, lines, line, 0f, 0f, w, h, callee)
    }

    /**
     * Moves pending lines right of [minX] (keeping their order) with jogs just below [fromY], the
     * rightmost first, so no two jogs cross. Uses `pend.size × jog` of height below [fromY].
     */
    private fun reroute(pend: List<Pending>, fromY: Float, minX: Float) {
        val sorted = pend.sortedBy { it.x }
        var limit = minX
        // A line already within half a lane of the limit stays put: [minX] has that much slack, and a
        // tiny jog would only look like a glitch.
        val targets = sorted.map { p -> (if (p.x >= limit - lane / 2) p.x else limit).also { limit = it + lane } }
        for (k in sorted.indices.reversed()) {
            val p = sorted[k]
            if (targets[k] == p.x) continue
            val jy = fromY + jog * (sorted.size - k)
            p.down(jy)
            p.to(targets[k], jy)
        }
    }

    private fun Box.placeInto(parent: Box, dx: Float, dy: Float): List<Pending> {
        nodes.forEach { parent.nodes += it.copy(x = it.x + dx, y = it.y + dy) }
        edges.forEach { e ->
            parent.edges += e.copy(
                points = e.points.map { FlowPoint(it.x + dx, it.y + dy) },
                labelAt = e.labelAt?.let { FlowPoint(it.x + dx, it.y + dy) },
            )
        }
        return pendings.map { pd -> Pending(pd.kind, pd.path.mapTo(mutableListOf()) { FlowPoint(it.x + dx, it.y + dy) }) }
    }

    private fun Box.line(
        vararg points: FlowPoint,
        arrow: Boolean,
        label: EdgeLabel? = null,
        labelAt: FlowPoint? = null,
        labelAlignEnd: Boolean = false,
    ) = edge(points.toList(), arrow, label, labelAt, labelAlignEnd)

    private fun Box.edge(
        points: List<FlowPoint>,
        arrow: Boolean,
        label: EdgeLabel? = null,
        labelAt: FlowPoint? = null,
        labelAlignEnd: Boolean = false,
    ) {
        val clean = mutableListOf<FlowPoint>()
        points.forEach { if (clean.lastOrNull() != it) clean += it }
        if (clean.size >= 2) edges += FlowEdge(clean, arrow, label, labelAt, labelAlignEnd)
    }

    private fun pt(x: Float, y: Float) = FlowPoint(x, y)

    /** Shifts everything into 0..width × 0..height with a margin and numbers the symbols. */
    private fun finish(box: Box): Flowchart {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        fun take(x: Float, y: Float) { minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y) }
        box.nodes.forEach { take(it.x - it.width / 2, it.y); take(it.x + it.width / 2, it.y + it.height) }
        box.edges.forEach { e -> e.points.forEach { take(it.x, it.y) } }
        val dx = margin - minX
        val dy = margin - minY
        val nodes = box.nodes.mapIndexed { i, n -> n.copy(id = i, x = n.x + dx, y = n.y + dy) }
        val edges = box.edges.map { e ->
            e.copy(points = e.points.map { FlowPoint(it.x + dx, it.y + dy) }, labelAt = e.labelAt?.let { FlowPoint(it.x + dx, it.y + dy) })
        }
        return Flowchart(nodes, edges, maxX - minX + 2 * margin, maxY - minY + 2 * margin)
    }

    companion object {
        /** An input/output parallelogram leans by this share of its height on each side. */
        const val IO_SKEW = 0.3f
        /** A call symbol's side bars sit this many character widths in from its edges. */
        const val CALL_BAR_CHARS = 0.9f
    }
}
