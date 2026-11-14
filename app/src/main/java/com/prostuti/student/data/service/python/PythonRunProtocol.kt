package com.prostuti.student.data.service.python

/** Messages between the app and [PythonRunService] (which lives in the `:py` process). */
internal object PythonRunProtocol {
    /** App → service: run `KEY_CODE` (traced when `arg1` = 1); replies go to `replyTo`. */
    const val MSG_RUN = 1
    /** App → service: a line for `input()` in `KEY_TEXT`. */
    const val MSG_INPUT = 2
    /** App → service: what a paused traced run does next, `KEY_TEXT` = [TRACE_STEP] or [TRACE_RUN]. */
    const val MSG_TRACE = 3
    /** Service → app: running, `arg1` = the service's process id (so the app can kill it). */
    const val MSG_STARTED = 10
    /** Service → app: printed text in `KEY_TEXT`, `arg1` = 1 for stderr. */
    const val MSG_OUTPUT = 11
    /** Service → app: the program waits for input. */
    const val MSG_INPUT_REQUEST = 12
    /** Service → app: finished; `KEY_ERROR` holds the traceback, absent on success. */
    const val MSG_DONE = 13
    /** Service → app: a traced run paused; `KEY_TEXT` is the step as JSON (see prostuti_runner.py). */
    const val MSG_PAUSED = 14

    const val TRACE_STEP = "step"
    const val TRACE_RUN = "run"

    const val KEY_CODE = "code"
    const val KEY_TEXT = "text"
    const val KEY_ERROR = "error"

    /** In the on-demand module (via :pyruntime); reached by reflection. */
    const val ENGINE_CLASS = "com.prostuti.pyruntime.PyEngine"
    const val MODULE = "pyfeature"
}
