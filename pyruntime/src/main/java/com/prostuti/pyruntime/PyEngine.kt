package com.prostuti.pyruntime

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Runs Python code with Chaquopy. Only JDK types cross into here, because the base app calls it
 * by reflection (it cannot depend on the on-demand module that carries this library).
 */
object PyEngine {

    /**
     * Runs [code] to the end. [output] gets each piece of printed text and whether it went to
     * stderr; [readLine] is asked for a line whenever the code calls `input()` (null = end of input).
     * With [step] the run is traced: before each line it gets the stack and variables as JSON and
     * answers "step" or "run" (run to the end). Returns null on success or the error traceback.
     */
    @JvmStatic
    fun run(
        context: Context,
        code: String,
        output: BiConsumer<String, Boolean>,
        readLine: Function<String, String?>,
        step: Function<String, String>?,
    ): String? {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
        val result = Python.getInstance().getModule("prostuti_runner").callAttr("run", code, output, readLine, step)
        return result?.toString()
    }
}
