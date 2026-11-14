package com.prostuti.student.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import android.os.SystemClock
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
import com.prostuti.student.data.service.python.PythonRunProtocol.TRACE_STEP
import com.prostuti.student.data.service.python.PythonRunService
import com.prostuti.student.domain.code.c.run.CCompiler
import com.prostuti.student.domain.code.c.run.CInterpreter
import com.prostuti.student.domain.code.c.run.CIo
import com.prostuti.student.domain.code.c.run.CProgramException
import com.prostuti.student.domain.code.c.run.CStop
import com.prostuti.student.domain.code.c.run.CTracer
import com.prostuti.student.domain.code.run.RunEvent
import com.prostuti.student.domain.code.run.RunFailure
import com.prostuti.student.domain.code.run.RunLimits
import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceStep
import kotlinx.serialization.json.Json
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs C in-process with the built-in interpreter on its own thread, and Python through
 * [PythonRunService] in the `:py` process. A run that does not end normally
 * (stopped, over the time or output limit) kills that process; a normal run leaves it warm, so the
 * next run skips Python's start-up. A traced run pauses before each line and waits for
 * [sendTraceCommand]; like waiting for input, a pause does not count toward the time limit.
 */
class CodeRunRepositoryImpl(private val context: Context) : CodeRunRepository {

    @Volatile private var service: Messenger? = null
    @Volatile private var killedAt = 0L
    /** Input for the running C program, if one is running. */
    @Volatile private var cInputs: LinkedBlockingQueue<Any>? = null
    /** Trace commands for the running C program, if one is traced. */
    @Volatile private var cCommands: LinkedBlockingQueue<TraceCommand>? = null
    /** The program waits for the student (input or a paused trace): the time limit does not run. */
    private val waitingInput = AtomicBoolean(false)
    private val json = Json { ignoreUnknownKeys = true }

    override fun runPython(code: String, trace: Boolean): Flow<RunEvent> = callbackFlow {
        val pid = AtomicInteger(0)
        val ended = AtomicBoolean(false)
        var printed = 0
        waitingInput.set(false)

        fun end(event: RunEvent) {
            if (ended.compareAndSet(false, true)) { trySend(event); close() }
        }

        val replies = Messenger(Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                MSG_STARTED -> pid.set(msg.arg1)
                MSG_OUTPUT -> if (!ended.get()) {
                    val text = msg.data.getString(KEY_TEXT).orEmpty()
                    printed += text.length
                    trySend(RunEvent.Output(text, isError = msg.arg1 == 1))
                    if (printed > RunLimits.OUTPUT_CHARS) end(RunEvent.Failed(RunFailure.OUTPUT_LIMIT))
                }
                MSG_INPUT_REQUEST -> if (!ended.get()) { waitingInput.set(true); trySend(RunEvent.InputRequested) }
                MSG_PAUSED -> if (!ended.get()) {
                    waitingInput.set(true)
                    val text = msg.data.getString(KEY_TEXT).orEmpty()
                    val step = runCatching { json.decodeFromString<TraceStep>(text) }.getOrElse { TraceStep(0, emptyList()) }
                    trySend(RunEvent.Paused(step))
                }
                MSG_DONE -> {
                    // Mark it a normal end first, so the process is kept.
                    if (ended.compareAndSet(false, true)) {
                        pid.set(0)
                        trySend(RunEvent.Finished(msg.data.getString(KEY_ERROR)))
                        close()
                    }
                }
            }
            true
        })
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
                val messenger = Messenger(binder)
                service = messenger
                runCatching { binder.linkToDeath({ end(RunEvent.Failed(RunFailure.CRASHED)) }, 0) }
                try {
                    messenger.send(Message.obtain(null, MSG_RUN, if (trace) 1 else 0, 0).apply {
                        replyTo = replies
                        data = Bundle().apply { putString(KEY_CODE, code) }
                    })
                } catch (_: RemoteException) {
                    end(RunEvent.Failed(RunFailure.CRASHED))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) = end(RunEvent.Failed(RunFailure.CRASHED))
        }
        // Right after a kill, the dying process could still answer the bind: give it a moment.
        val binding = launch {
            val wait = killedAt + KILL_SETTLE_MS - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            context.bindService(Intent(context, PythonRunService::class.java), connection, Context.BIND_AUTO_CREATE)
        }

        // The time limit counts running time only, not time spent waiting for the student's input.
        val clock = launch {
            var used = 0L
            while (used < RunLimits.TIME_MS) {
                delay(TICK_MS)
                if (!waitingInput.get()) used += TICK_MS
            }
            end(RunEvent.Failed(RunFailure.TIME_LIMIT))
        }

        awaitClose {
            binding.cancel()
            clock.cancel()
            service = null
            runCatching { context.unbindService(connection) }
            // Anything but a normal finish (stop button, limits, leaving the screen) kills the program.
            pid.get().takeIf { it != 0 }?.let {
                Process.killProcess(it)
                killedAt = SystemClock.elapsedRealtime()
            }
        }
    }

    override fun runC(code: String, trace: Boolean): Flow<RunEvent> = callbackFlow {
        val ended = AtomicBoolean(false)
        val stop = AtomicBoolean(false)
        val printed = AtomicInteger(0)
        val inputs = LinkedBlockingQueue<Any>()
        val commands = LinkedBlockingQueue<TraceCommand>()
        cCommands = commands
        val lock = Any()
        val pending = StringBuilder()
        waitingInput.set(false)
        cInputs = inputs

        fun end(event: RunEvent) {
            if (ended.compareAndSet(false, true)) { stop.set(true); trySend(event); close() }
        }

        fun flush() {
            val text = synchronized(lock) { pending.toString().also { pending.setLength(0) } }
            if (text.isEmpty() || ended.get()) return
            trySend(RunEvent.Output(text, isError = false))
            if (printed.addAndGet(text.length) > RunLimits.OUTPUT_CHARS) end(RunEvent.Failed(RunFailure.OUTPUT_LIMIT))
        }

        val io = object : CIo {
            override fun write(text: String) {
                val size = synchronized(lock) { pending.append(text); pending.length }
                if (size > RunLimits.OUTPUT_CHARS) flush()
            }

            override fun readLine(): String? {
                flush()
                waitingInput.set(true)
                trySend(RunEvent.InputRequested)
                val line = try { inputs.take() } catch (_: InterruptedException) { throw CStop() }
                return line as? String
            }

            override val stopped: Boolean get() = stop.get()
        }
        val tracer = if (!trace) null else CTracer { step ->
            flush()
            waitingInput.set(true)
            trySend(RunEvent.Paused(step))
            try { commands.take() } catch (_: InterruptedException) { throw CStop() }
        }
        val program = Thread(null, {
            val event = try {
                CInterpreter(io, tracer).run(CCompiler.compile(code))
                RunEvent.Finished(null)
            } catch (e: CProgramException) {
                RunEvent.Finished(e.report(), e.line)
            } catch (_: CStop) {
                null
            } catch (_: StackOverflowError) {
                RunEvent.Finished("runtime error: too many nested function calls\n")
            }
            flush()
            event?.let { end(it) }
        }, "c-program", C_STACK_BYTES)
        program.start()

        val flusher = launch {
            while (true) { delay(FLUSH_MS); flush() }
        }
        val clock = launch {
            var used = 0L
            while (used < RunLimits.TIME_MS) {
                delay(TICK_MS)
                if (!waitingInput.get()) used += TICK_MS
            }
            flush()
            end(RunEvent.Failed(RunFailure.TIME_LIMIT))
        }
        awaitClose {
            stop.set(true)
            program.interrupt()
            flusher.cancel()
            clock.cancel()
            if (cInputs === inputs) cInputs = null
            if (cCommands === commands) cCommands = null
        }
    }

    override fun sendInput(line: String) {
        waitingInput.set(false)
        cInputs?.let { it.put(line); return }
        try {
            service?.send(Message.obtain(null, MSG_INPUT).apply { data = Bundle().apply { putString(KEY_TEXT, line) } })
        } catch (_: RemoteException) {
            // The process died; the run's flow reports it.
        }
    }

    override fun sendTraceCommand(command: TraceCommand) {
        waitingInput.set(false)
        cCommands?.let { it.put(command); return }
        try {
            service?.send(Message.obtain(null, MSG_TRACE).apply {
                data = Bundle().apply { putString(KEY_TEXT, if (command == TraceCommand.STEP) TRACE_STEP else TRACE_RUN) }
            })
        } catch (_: RemoteException) {
            // The process died; the run's flow reports it.
        }
    }

    private companion object {
        const val TICK_MS = 250L
        const val KILL_SETTLE_MS = 600L
        const val FLUSH_MS = 40L
        /** Deep recursion in student code needs more than a thread's default stack. */
        const val C_STACK_BYTES = 32L * 1024 * 1024
    }
}
