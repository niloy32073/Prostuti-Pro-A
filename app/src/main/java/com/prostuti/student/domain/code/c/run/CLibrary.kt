package com.prostuti.student.domain.code.c.run

import kotlin.math.abs

/** The C library functions ([Builtin]) on top of the interpreter's memory. */
internal class CLibrary(private val vm: CInterpreter, private val io: CIo, private val input: CInput) {

    private var seed = 1L

    /** [args] are evaluated; [exprs] give their static types (and lines). */
    fun call(b: Builtin, args: List<Any>, exprs: List<CExpr>, line: Int): Any = when (b) {
        Builtin.PRINTF -> {
            val text = format(args, exprs, 0, line)
            io.write(text)
            text.length.toLong()
        }
        Builtin.SPRINTF -> {
            val text = format(args, exprs, 1, line)
            writeString(args[0] as CPtr, text, line, "sprintf")
            text.length.toLong()
        }
        Builtin.FPRINTF -> {
            val text = format(args, exprs, 1, line)
            if (args[0] == 0L) vm.fail(line, "fprintf cannot write to stdin")
            io.write(text)
            text.length.toLong()
        }
        Builtin.SCANF -> scanf(args, exprs, line)
        Builtin.PUTS -> { io.write(readString(args[0] as CPtr, line) + "\n"); 1L }
        Builtin.PUTCHAR -> { io.write(((args[0] as Long) and 0xFFFF).toInt().toChar().toString()); args[0] }
        Builtin.GETCHAR, Builtin.GETCH, Builtin.GETCHE -> input.next().toLong()
        Builtin.GETS -> {
            val text = readLine(Int.MAX_VALUE, keepNewline = false)
            if (text == null) CPtr.NULL else { writeString(args[0] as CPtr, text, line, "gets"); args[0] }
        }
        Builtin.FGETS -> {
            val n = (args[1] as Long).toInt()
            if (args[2] != 0L) vm.fail(line, "fgets can only read from stdin here")
            if (n <= 1) vm.fail(line, "fgets needs room for at least one character")
            val text = readLine(n - 1, keepNewline = true)
            if (text == null) CPtr.NULL else { writeString(args[0] as CPtr, text, line, "fgets"); args[0] }
        }
        Builtin.FFLUSH -> 0L
        Builtin.CLRSCR -> 0L

        Builtin.SQRT -> math(args) { kotlin.math.sqrt(it) }
        Builtin.CBRT -> math(args) { Math.cbrt(it) }
        Builtin.POW -> Math.pow(args[0] as Double, args[1] as Double)
        Builtin.FABS -> math(args) { abs(it) }
        Builtin.FLOOR -> math(args) { kotlin.math.floor(it) }
        Builtin.CEIL -> math(args) { kotlin.math.ceil(it) }
        Builtin.ROUND -> math(args) { if (it < 0) -kotlin.math.floor(-it + 0.5) else kotlin.math.floor(it + 0.5) }
        Builtin.TRUNC -> math(args) { kotlin.math.truncate(it) }
        Builtin.SIN -> math(args) { kotlin.math.sin(it) }
        Builtin.COS -> math(args) { kotlin.math.cos(it) }
        Builtin.TAN -> math(args) { kotlin.math.tan(it) }
        Builtin.ASIN -> math(args) { kotlin.math.asin(it) }
        Builtin.ACOS -> math(args) { kotlin.math.acos(it) }
        Builtin.ATAN -> math(args) { kotlin.math.atan(it) }
        Builtin.ATAN2 -> kotlin.math.atan2(args[0] as Double, args[1] as Double)
        Builtin.EXP -> math(args) { kotlin.math.exp(it) }
        Builtin.LOG -> math(args) { kotlin.math.ln(it) }
        Builtin.LOG10 -> math(args) { kotlin.math.log10(it) }
        Builtin.LOG2 -> math(args) { kotlin.math.log2(it) }
        Builtin.FMOD -> (args[0] as Double).rem(args[1] as Double)
        Builtin.HYPOT -> kotlin.math.hypot(args[0] as Double, args[1] as Double)

        Builtin.ABS -> CProgramParser.wrap(abs(args[0] as Long), CType.INT)
        Builtin.LABS -> abs(args[0] as Long)
        Builtin.RAND -> {
            // A linear congruential generator, like many C libraries (deterministic per seed).
            seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
            seed
        }
        Builtin.SRAND -> { seed = args[0] as Long; 0L }
        Builtin.TIME -> System.currentTimeMillis() / 1000
        Builtin.EXIT -> throw CExit(args[0] as Long)
        Builtin.MALLOC, Builtin.CALLOC -> {
            val bytes = if (b == Builtin.MALLOC) args[0] as Long else (args[0] as Long) * (args[1] as Long)
            if (bytes <= 0 || bytes > 64L * 1024 * 1024) {
                CPtr.NULL
            } else {
                val obj = vm.newObj("malloc", 0, 1, line)
                obj.untypedBytes = bytes
                obj.zeroWhenTyped = b == Builtin.CALLOC
                CPtr(obj, 0)
            }
        }
        Builtin.FREE -> {
            val p = args[0] as CPtr
            val obj = p.obj
            if (obj != null) {
                if (obj.name != "malloc") vm.fail(line, "free() is only for memory from malloc()/calloc()")
                if (obj.freed) vm.fail(line, "this memory was already freed")
                if (p.index != 0) vm.fail(line, "free() needs the pointer malloc() returned, not one moved inside it")
                obj.freed = true
            }
            0L
        }
        Builtin.ATOI -> readString(args[0] as CPtr, line).trim().let { s ->
            Regex("^[+-]?\\d+").find(s)?.value?.toLongOrNull()?.let { CProgramParser.wrap(it, CType.INT) } ?: 0L
        }
        Builtin.ATOF -> readString(args[0] as CPtr, line).trim().let { s ->
            Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?").find(s)?.value?.toDoubleOrNull() ?: 0.0
        }

        Builtin.STRLEN -> readString(args[0] as CPtr, line).length.toLong()
        Builtin.STRCPY -> { writeString(args[0] as CPtr, readString(args[1] as CPtr, line), line, "strcpy"); args[0] }
        Builtin.STRNCPY -> {
            val n = (args[2] as Long).toInt()
            val src = readString(args[1] as CPtr, line).take(n)
            val dst = args[0] as CPtr
            for (i in 0 until n) storeChar(dst, i, if (i < src.length) src[i].code.toLong() else 0L, line, "strncpy")
            dst
        }
        Builtin.STRCAT -> {
            val dst = args[0] as CPtr
            writeString(dst, readString(dst, line) + readString(args[1] as CPtr, line), line, "strcat")
            dst
        }
        Builtin.STRNCAT -> {
            val dst = args[0] as CPtr
            writeString(dst, readString(dst, line) + readString(args[1] as CPtr, line).take((args[2] as Long).toInt()), line, "strncat")
            dst
        }
        Builtin.STRCMP -> compare(readString(args[0] as CPtr, line), readString(args[1] as CPtr, line))
        Builtin.STRNCMP -> {
            val n = (args[2] as Long).toInt()
            compare(readString(args[0] as CPtr, line).take(n), readString(args[1] as CPtr, line).take(n))
        }
        Builtin.STRREV -> transform(args[0] as CPtr, line) { it.reversed() }
        Builtin.STRUPR -> transform(args[0] as CPtr, line) { it.uppercase() }
        Builtin.STRLWR -> transform(args[0] as CPtr, line) { it.lowercase() }

        Builtin.ISDIGIT -> ctype(args) { it in '0'..'9' }
        Builtin.ISALPHA -> ctype(args) { it in 'a'..'z' || it in 'A'..'Z' }
        Builtin.ISALNUM -> ctype(args) { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' }
        Builtin.ISUPPER -> ctype(args) { it in 'A'..'Z' }
        Builtin.ISLOWER -> ctype(args) { it in 'a'..'z' }
        Builtin.ISSPACE -> ctype(args) { it == ' ' || it in '\t'..'\r' }
        Builtin.ISPUNCT -> ctype(args) { it.code in 33..126 && !it.isLetterOrDigit() }
        Builtin.TOUPPER -> (args[0] as Long).let { if (it in 'a'.code.toLong()..'z'.code.toLong()) it - 32 else it }
        Builtin.TOLOWER -> (args[0] as Long).let { if (it in 'A'.code.toLong()..'Z'.code.toLong()) it + 32 else it }
    }

    private inline fun math(args: List<Any>, f: (Double) -> Double): Double = f(args[0] as Double)

    private inline fun ctype(args: List<Any>, f: (Char) -> Boolean): Long {
        val c = args[0] as Long
        return if (c in 0..127 && f(c.toInt().toChar())) 1L else 0L
    }

    private fun compare(a: String, b: String): Long {
        val c = a.compareTo(b)
        return when { c < 0 -> -1L; c > 0 -> 1L; else -> 0L }
    }

    private fun transform(p: CPtr, line: Int, f: (String) -> String): CPtr {
        writeString(p, f(readString(p, line)), line, "string function")
        return p
    }

    private fun format(args: List<Any>, exprs: List<CExpr>, fmtIndex: Int, line: Int): String {
        val fmt = readString(args[fmtIndex] as CPtr, line)
        val rest = args.drop(fmtIndex + 1)
        val types = exprs.drop(fmtIndex + 1).map { it.type.decayed }
        return CPrintf.format(fmt, rest, types, line) { readString(it, line) }
    }

    // ── Strings in memory ──

    /** The characters from [p] up to the terminating '\0'. */
    fun readString(p: CPtr, line: Int): String {
        val obj = p.obj ?: vm.fail(line, "using a NULL pointer as a string")
        val sb = StringBuilder()
        var i = p.index
        while (true) {
            if (i < 0 || i >= obj.slots.size) vm.fail(line, "the string in '${obj.name}' has no ending '\\0'")
            val c = vm.load(obj, i, line) as? Long ?: vm.fail(line, "'${obj.name}' is not a string")
            if (c == 0L) return sb.toString()
            sb.append((c and 0xFFFF).toInt().toChar())
            i++
            if (sb.length > MAX_STRING) vm.fail(line, "the string is too long")
        }
    }

    /** Writes [text] and a '\0' at [p]; an error if it does not fit (C would overwrite memory). */
    private fun writeString(p: CPtr, text: String, line: Int, what: String) {
        val obj = p.obj ?: vm.fail(line, "$what: writing through a NULL pointer")
        if (p.index + text.length >= obj.slots.size) {
            vm.fail(line, "$what: the text needs ${text.length + 1} characters (with '\\0') but '${obj.name}' has room for ${obj.slots.size - p.index}")
        }
        for (i in text.indices) vm.store(obj, p.index + i, text[i].code.toLong(), line)
        vm.store(obj, p.index + text.length, 0L, line)
    }

    private fun storeChar(p: CPtr, i: Int, c: Long, line: Int, what: String) {
        val obj = p.obj ?: vm.fail(line, "$what: writing through a NULL pointer")
        if (p.index + i >= obj.slots.size) vm.fail(line, "$what: the text does not fit in '${obj.name}'")
        vm.store(obj, p.index + i, c, line)
    }

    // ── Input ──

    /** A line from stdin (up to [max] characters), or null at the end of input. */
    private fun readLine(max: Int, keepNewline: Boolean): String? {
        if (input.peek() < 0) return null
        val sb = StringBuilder()
        while (sb.length < max) {
            val c = input.peek()
            if (c < 0) break
            input.next()
            if (c == '\n'.code) { if (keepNewline) sb.append('\n'); break }
            sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun scanf(args: List<Any>, exprs: List<CExpr>, line: Int): Long {
        val fmt = readString(args[0] as CPtr, line)
        var argIndex = 1
        var count = 0L
        var i = 0
        fun eofResult() = if (count == 0L) -1L else count
        while (i < fmt.length) {
            val c = fmt[i]
            if (c.isWhitespace()) { input.skipSpaces(); i++; continue }
            if (c != '%') {
                if (input.peek() == c.code) { input.next(); i++; continue }
                return if (input.peek() < 0) eofResult() else count
            }
            i++
            if (i < fmt.length && fmt[i] == '%') {
                input.skipSpaces()
                if (input.peek() == '%'.code) { input.next(); i++; continue } else return count
            }
            var suppress = false
            if (i < fmt.length && fmt[i] == '*') { suppress = true; i++ }
            var width = 0
            while (i < fmt.length && fmt[i].isDigit()) { width = width * 10 + (fmt[i] - '0'); i++ }
            val lenStart = i
            while (i < fmt.length && fmt[i] in "hlLqjzt") i++
            val length = fmt.substring(lenStart, i)
            if (i >= fmt.length) vm.fail(line, "scanf: the format ends inside a % specifier")
            val conv = fmt[i]
            i++
            var scanset: String? = null
            if (conv == '[') {
                val close = fmt.indexOf(']', if (i < fmt.length && fmt[i] == '^') i + 2 else i + 1)
                if (close < 0) vm.fail(line, "scanf: %[ is missing ']'")
                scanset = fmt.substring(i, close)
                i = close + 1
            }
            val spec = "%$length$conv"
            val target: Pair<CPtr, CType>? = if (suppress) null else {
                if (argIndex >= args.size) vm.fail(line, "scanf: the format has more % specifiers than variables (nothing for $spec)")
                val type = exprs[argIndex].type.decayed as CType.Ptr
                (args[argIndex] as CPtr to type.to).also { argIndex++ }
            }
            target?.let { checkScanTarget(conv, length, it.second, spec, line) }
            when (conv) {
                'd', 'i', 'u', 'x', 'X', 'o' -> {
                    input.skipSpaces()
                    val radix = when (conv) { 'x', 'X' -> 16; 'o' -> 8; else -> 10 }
                    val v = readInteger(radix, conv == 'i', width) ?: return if (input.peek() < 0) eofResult() else count
                    target?.let { store(it, v, line) }
                }
                'f', 'e', 'E', 'g', 'G', 'a' -> {
                    input.skipSpaces()
                    val v = readFloat(width) ?: return if (input.peek() < 0) eofResult() else count
                    target?.let { store(it, v, line) }
                }
                'c' -> {
                    val n = if (width == 0) 1 else width
                    val chars = StringBuilder()
                    repeat(n) {
                        val ch = input.next()
                        if (ch < 0) return eofResult()
                        chars.append(ch.toChar())
                    }
                    target?.let { (p, _) -> chars.forEachIndexed { k, ch -> storeChar(p, k, ch.code.toLong(), line, "scanf %c") } }
                }
                's', '[' -> {
                    if (conv == 's') input.skipSpaces()
                    val sb = StringBuilder()
                    val max = if (width == 0) Int.MAX_VALUE else width
                    while (sb.length < max) {
                        val ch = input.peek()
                        if (ch < 0) break
                        val ok = if (conv == 's') !Character.isWhitespace(ch) else inScanset(ch.toChar(), scanset!!)
                        if (!ok) break
                        sb.append(ch.toChar())
                        input.next()
                    }
                    if (sb.isEmpty()) return if (input.peek() < 0) eofResult() else count
                    target?.let { writeString(it.first, sb.toString(), line, "scanf $spec") }
                }
                'n' -> vm.fail(line, "scanf: %n is not supported")
                else -> vm.fail(line, "scanf: unknown format $spec")
            }
            if (!suppress) count++
        }
        return count
    }

    private fun checkScanTarget(conv: Char, length: String, target: CType, spec: String, line: Int) {
        when (conv) {
            'd', 'i', 'u', 'x', 'X', 'o' -> {
                if (!target.isInteger) {
                    val hint = when (target) {
                        CType.FLOAT -> " (use %f for float)"
                        CType.DOUBLE -> " (use %lf for double)"
                        else -> ""
                    }
                    vm.fail(line, "scanf: $spec needs the address of a whole-number variable, but this is a $target$hint")
                }
                val kind = (target as CType.Num).kind
                val expected = when (length) {
                    "hh" -> setOf(NumKind.CHAR)
                    "h" -> setOf(NumKind.SHORT)
                    "l", "ll", "q", "j", "z", "t" -> setOf(NumKind.LONG, NumKind.LLONG)
                    else -> setOf(NumKind.INT, NumKind.BOOL)
                }
                if (kind !in expected) {
                    val right = when (kind) {
                        NumKind.CHAR -> "%hhd (or %c for a character)"
                        NumKind.SHORT -> "%hd"
                        NumKind.LONG, NumKind.LLONG -> "%ld"
                        else -> "%d"
                    }
                    vm.fail(line, "scanf: $spec does not match a $target variable; use $right")
                }
            }
            'f', 'e', 'E', 'g', 'G', 'a' -> {
                val ok = when (length) {
                    "" -> target == CType.FLOAT
                    "l" -> target == CType.DOUBLE
                    "L" -> target is CType.Num && target.kind == NumKind.LDOUBLE
                    else -> false
                }
                if (!ok) {
                    val right = when {
                        target == CType.FLOAT -> "%f"
                        target == CType.DOUBLE -> "%lf"
                        target is CType.Num && target.kind == NumKind.LDOUBLE -> "%Lf"
                        else -> null
                    }
                    if (right == null) vm.fail(line, "scanf: $spec needs the address of a float or double variable, but this is a $target")
                    vm.fail(line, "scanf: $spec does not match a $target variable; use $right")
                }
            }
            'c', 's', '[' -> if ((target.baseScalar as? CType.Num)?.kind != NumKind.CHAR) {
                vm.fail(line, "scanf: $spec needs a char array (or &ch for %c), but this is a $target")
            }
        }
    }

    private fun store(target: Pair<CPtr, CType>, value: Any, line: Int) {
        val (p, t) = target
        val obj = p.obj ?: vm.fail(line, "scanf: writing through a NULL pointer")
        val converted = vm.convert(value, if (value is Double) CType.DOUBLE else CType.LONG, t, line)
        vm.store(obj, p.index, converted, line)
    }

    private fun readInteger(radix: Int, autoBase: Boolean, width: Int): Long? {
        val max = if (width == 0) Int.MAX_VALUE else width
        val sb = StringBuilder()
        fun peekChar() = input.peek().let { if (it < 0) null else it.toChar() }
        peekChar()?.let { if (it == '+' || it == '-') { sb.append(it); input.next() } }
        var base = radix
        if (autoBase || radix == 16) {
            if (peekChar() == '0') {
                sb.append('0'); input.next()
                val x = peekChar()
                if ((x == 'x' || x == 'X') && sb.length < max) { input.next(); sb.setLength(sb.length - 1); base = 16 }
                else if (autoBase) base = 8
            }
        }
        while (sb.length < max) {
            val ch = peekChar() ?: break
            if (Character.digit(ch, base) < 0) break
            sb.append(ch)
            input.next()
        }
        val digits = sb.toString()
        if (digits.isEmpty() || digits == "+" || digits == "-") return null
        return digits.toBigInteger(base).toLong()
    }

    private fun readFloat(width: Int): Double? {
        val max = if (width == 0) Int.MAX_VALUE else width
        val sb = StringBuilder()
        fun peekChar() = input.peek().let { if (it < 0) null else it.toChar() }
        fun take() { sb.append(input.next().toChar()) }
        if (peekChar() == '+' || peekChar() == '-') take()
        while (sb.length < max && peekChar()?.isDigit() == true) take()
        if (sb.length < max && peekChar() == '.') {
            take()
            while (sb.length < max && peekChar()?.isDigit() == true) take()
        }
        if (sb.none { it.isDigit() }) return null
        if (sb.length < max && (peekChar() == 'e' || peekChar() == 'E')) {
            take()
            if (peekChar() == '+' || peekChar() == '-') take()
            while (sb.length < max && peekChar()?.isDigit() == true) take()
        }
        return sb.toString().trimEnd('e', 'E', '+', '-').toDoubleOrNull()
    }

    private fun inScanset(c: Char, set: String): Boolean {
        val negate = set.startsWith("^")
        val body = if (negate) set.substring(1) else set
        var found = false
        var i = 0
        while (i < body.length) {
            if (i + 2 < body.length && body[i + 1] == '-') {
                if (c in body[i]..body[i + 2]) found = true
                i += 3
            } else {
                if (c == body[i]) found = true
                i++
            }
        }
        return found != negate
    }

    private companion object {
        const val MAX_STRING = 1_000_000
    }
}
