package com.prostuti.student.domain.code.run

/**
 * Which values changed between two paused steps, so the variables panel can mark them. A value is
 * named by its path: `frame index/function/name/child/…` for the stack and `globals/name/…` for
 * globals. A variable that appears (a new frame, a new declaration) counts as changed.
 */
object TraceChanges {

    fun path(frameIndex: Int, frame: TraceFrame, vararg names: String) = listOf("$frameIndex", frame.function, *names).joinToString("/")

    fun changed(previous: TraceStep?, next: TraceStep): Set<String> {
        if (previous == null) return emptySet()
        val before = HashMap<String, String?>()
        flatten(previous, before)
        val after = HashMap<String, String?>()
        flatten(next, after)
        return after.filter { (k, v) -> !before.containsKey(k) || before[k] != v }.keys
    }

    private fun flatten(step: TraceStep, out: MutableMap<String, String?>) {
        step.frames.forEachIndexed { i, f -> f.vars.forEach { add(it, path(i, f), out) } }
        step.globals.forEach { add(it, "globals", out) }
    }

    private fun add(v: TraceVar, prefix: String, out: MutableMap<String, String?>) {
        val key = "$prefix/${v.name}"
        out[key] = v.value
        v.children.forEach { add(it, key, out) }
    }
}
