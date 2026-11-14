"""Runs one student program: output goes to a Java callback, input() asks the app for a line.

Returns None when the program ends normally, or the traceback text (without this file's own
frames) when it raises. With a step callback the program is traced: before each line of the
student's code it pauses and sends the call stack and variables (as JSON) to the callback, which
answers "step" (pause at the next line) or "run" (stop tracing and run to the end).
"""
import builtins
import json
import sys
import traceback
import types

FILENAME = "main.py"


class _Stream:
    def __init__(self, sink, is_error):
        self._sink = sink
        self._is_error = is_error

    def write(self, text):
        text = str(text)
        if text:
            self._sink.accept(text, self._is_error)
        return len(text)

    def flush(self):
        pass

    def isatty(self):
        return False


MAX_ITEMS = 50
MAX_DEPTH = 3
MAX_REPR = 120
_SKIP_TYPES = (types.ModuleType, types.FunctionType, types.BuiltinFunctionType, types.MethodType, type)


def _short(text):
    return text if len(text) <= MAX_REPR else text[:MAX_REPR] + "…"


def _value(name, v, depth):
    try:
        if type(v).__repr__ is object.__repr__:
            text = type(v).__name__ + " object"
        else:
            text = _short(repr(v))
    except Exception:
        text = "?"
    children = []
    if depth < MAX_DEPTH:
        try:
            if isinstance(v, (list, tuple)):
                children = [_value("[%d]" % i, x, depth + 1) for i, x in enumerate(v[:MAX_ITEMS])]
            elif isinstance(v, dict):
                children = [_value("[%s]" % _short(repr(k)), x, depth + 1) for k, x in list(v.items())[:MAX_ITEMS]]
            elif hasattr(v, "__dict__") and not isinstance(v, _SKIP_TYPES):
                children = [_value(k, x, depth + 1) for k, x in list(vars(v).items())[:MAX_ITEMS] if not k.startswith("__")]
        except Exception:
            children = []
    return {"name": name, "value": text, "children": children}


def _frame_vars(frame):
    out = []
    for k, v in list(frame.f_locals.items()):
        if k.startswith("__") or isinstance(v, _SKIP_TYPES):
            continue
        out.append(_value(k, v, 0))
    return out


def _is_student_frame(frame):
    code = frame.f_code
    # Comprehensions, lambdas and generator expressions run inside the line that made them.
    return code.co_filename == FILENAME and (code.co_name == "<module>" or not code.co_name.startswith("<"))


def _snapshot(frame):
    frames = []
    f = frame
    while f is not None:
        if _is_student_frame(f):
            frames.append(f)
        f = f.f_back
    frames.reverse()
    out = []
    for fr in frames:
        name = fr.f_code.co_name
        if name == "<module>":
            name = "main"
        else:
            name = getattr(fr.f_code, "co_qualname", name).replace(".<locals>.", ".")
        out.append({"function": name, "line": fr.f_lineno, "vars": _frame_vars(fr)})
    return {"line": frame.f_lineno, "frames": out}


def _tracer(step_sink):
    state = {"on": True}

    def local(frame, event, arg):
        if not state["on"]:
            return None
        if event == "line":
            answer = step_sink.apply(json.dumps(_snapshot(frame), ensure_ascii=False))
            if str(answer) == "run":
                state["on"] = False
                sys.settrace(None)
                return None
        return local

    def start(frame, event, arg):
        if not state["on"] or not _is_student_frame(frame):
            return None
        return local

    return start


def run(code, out_sink, read_line, step_sink=None):
    sys.stdout = _Stream(out_sink, False)
    sys.stderr = _Stream(out_sink, True)

    def _input(prompt=""):
        prompt = str(prompt)
        if prompt:
            sys.stdout.write(prompt)
        line = read_line.apply(prompt)
        if line is None:
            raise EOFError("EOF when reading a line")
        return str(line)

    builtins.input = _input
    scope = {"__name__": "__main__", "__builtins__": builtins}
    try:
        compiled = compile(code, FILENAME, "exec")
        if step_sink is not None:
            sys.settrace(_tracer(step_sink))
        try:
            exec(compiled, scope)
        finally:
            sys.settrace(None)
        return None
    except SystemExit as e:
        if e.code in (None, 0):
            return None
        return "SystemExit: " + str(e.code) + "\n"
    except SyntaxError as e:
        return "".join(traceback.format_exception_only(type(e), e))
    except BaseException as e:
        tb = e.__traceback__
        # Drop the frames of this runner, so the student sees only their own lines.
        while tb is not None and tb.tb_frame.f_code.co_filename != FILENAME:
            tb = tb.tb_next
        return "".join(traceback.format_exception(type(e), e, tb))
