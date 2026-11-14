package com.prostuti.student.data.service.python

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import com.prostuti.student.data.service.python.PythonRunProtocol.ENGINE_CLASS
import com.prostuti.student.data.service.python.PythonRunProtocol.KEY_CODE
import com.prostuti.student.data.service.python.PythonRunProtocol.KEY_ERROR
import com.prostuti.student.data.service.python.PythonRunProtocol.KEY_TEXT
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_DONE
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_INPUT
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_INPUT_REQUEST
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_OUTPUT
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_PAUSED
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_RUN
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_STARTED
import com.prostuti.student.data.service.python.PythonRunProtocol.MSG_TRACE
import com.prostuti.student.data.service.python.PythonRunProtocol.TRACE_RUN
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Runs one Python program at a time in the `:py` process, so a stuck or crashing program can be
 * killed without touching the app. Python itself comes from the on-demand module; this process is
 * only started after Play has installed it, so the module's code and native libraries load normally.
 * Printed text is batched (every 50 ms or 8 KB) to keep the message channel light.
 */
class PythonRunService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val inputs = LinkedBlockingQueue<Any>()
    private val traceCommands = LinkedBlockingQueue<String>()
    @Volatile private var client: Messenger? = null

    private val lock = Any()
    private val pending = StringBuilder()
    private var pendingIsError = false
    private var flushScheduled = false

    private val incoming = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MSG_RUN -> start(msg.replyTo, msg.data.getString(KEY_CODE).orEmpty(), trace = msg.arg1 == 1)
            MSG_INPUT -> inputs.put(msg.data.getString(KEY_TEXT) ?: NO_MORE_INPUT)
            MSG_TRACE -> traceCommands.put(msg.data.getString(KEY_TEXT) ?: TRACE_RUN)
        }
        true
    })

    override fun onBind(intent: Intent?): IBinder = incoming.binder

    private fun start(replyTo: Messenger, code: String, trace: Boolean) {
        client = replyTo
        inputs.clear()
        traceCommands.clear()
        val step = if (!trace) null else Function<String, String> { json ->
            flush()
            send(Message.obtain(null, MSG_PAUSED).apply { data = Bundle().apply { putString(KEY_TEXT, json) } })
            traceCommands.take()
        }
        send(Message.obtain(null, MSG_STARTED, Process.myPid(), 0))
        Thread(null, {
            val error = try {
                val run = Class.forName(ENGINE_CLASS).getMethod(
                    "run", Context::class.java, String::class.java, BiConsumer::class.java, Function::class.java, Function::class.java,
                )
                run.invoke(
                    null, applicationContext, code,
                    BiConsumer<String, Boolean> { text, isError -> output(text, isError) },
                    Function<String, String?> { _ ->
                        flush()
                        send(Message.obtain(null, MSG_INPUT_REQUEST))
                        inputs.take() as? String
                    },
                    step,
                ) as String?
            } catch (e: InvocationTargetException) {
                "Internal error: ${e.targetException}\n"
            } catch (e: ReflectiveOperationException) {
                "Internal error: $e\n"
            }
            flush()
            send(Message.obtain(null, MSG_DONE).apply { data = Bundle().apply { error?.let { putString(KEY_ERROR, it) } } })
        }, "python", PYTHON_STACK_BYTES).start()
    }

    private fun output(text: String, isError: Boolean) = synchronized(lock) {
        if (pending.isNotEmpty() && pendingIsError != isError) flushLocked()
        pending.append(text)
        pendingIsError = isError
        if (pending.length >= FLUSH_CHARS) {
            flushLocked()
        } else if (!flushScheduled) {
            flushScheduled = true
            main.postDelayed({ flush() }, FLUSH_DELAY_MS)
        }
    }

    private fun flush() = synchronized(lock) { flushLocked() }

    private fun flushLocked() {
        flushScheduled = false
        if (pending.isEmpty()) return
        val text = pending.toString()
        pending.setLength(0)
        send(Message.obtain(null, MSG_OUTPUT, if (pendingIsError) 1 else 0, 0).apply { data = Bundle().apply { putString(KEY_TEXT, text) } })
    }

    private fun send(message: Message) {
        try {
            client?.send(message)
        } catch (_: RemoteException) {
            // The app went away; it kills this process if the run should stop.
        }
    }

    private companion object {
        val NO_MORE_INPUT = Any()
        const val FLUSH_CHARS = 8 * 1024
        const val FLUSH_DELAY_MS = 50L
        /** Deep recursion (Python's default limit is 1000 frames) needs more than a thread's default stack. */
        const val PYTHON_STACK_BYTES = 16L * 1024 * 1024
    }
}
