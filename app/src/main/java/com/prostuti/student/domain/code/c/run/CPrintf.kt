package com.prostuti.student.domain.code.c.run

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * printf-style formatting as glibc does it (flags `-+ #0`, width, precision, `*`, length modifiers,
 * `d i u o x X f F e E g G c s p %`). Unlike C, a value of the wrong kind for its `%` (an int for
 * `%f`, a double for `%d`) is an error with a clear message instead of garbage output.
 */
internal object CPrintf {

    /** [args] are already promoted (double, int/long, pointers); [types] their static types. */
    fun format(fmt: String, args: List<Any>, types: List<CType>, line: Int, readString: (CPtr) -> String): String {
        val out = StringBuilder()
        var argIndex = 0
        fun nextArg(spec: String): Pair<Any, CType> {
            if (argIndex >= args.size) fail(line, "printf: the format has more % specifiers than values (no value for %$spec)")
            return args[argIndex] to types[argIndex].also { argIndex++ }
        }
        var i = 0
        while (i < fmt.length) {
            val c = fmt[i]
            if (c != '%') { out.append(c); i++; continue }
            i++
            if (i >= fmt.length) fail(line, "printf: the format ends with a lone %")
            if (fmt[i] == '%') { out.append('%'); i++; continue }
            val flags = StringBuilder()
            while (i < fmt.length && fmt[i] in "-+ #0") { flags.append(fmt[i]); i++ }
            var width = 0
            if (i < fmt.length && fmt[i] == '*') {
                val (w, t) = nextArg("*")
                if (!t.isInteger) fail(line, "printf: a * width needs an int value")
                width = (w as Long).toInt()
                if (width < 0) { flags.append('-'); width = -width }
                i++
            } else {
                while (i < fmt.length && fmt[i].isDigit()) { width = width * 10 + (fmt[i] - '0'); i++ }
            }
            var precision: Int? = null
            if (i < fmt.length && fmt[i] == '.') {
                i++
                if (i < fmt.length && fmt[i] == '*') {
                    val (pv, t) = nextArg("*")
                    if (!t.isInteger) fail(line, "printf: a .* precision needs an int value")
                    precision = (pv as Long).toInt().takeIf { it >= 0 }
                    i++
                } else {
                    var pr = 0
                    while (i < fmt.length && fmt[i].isDigit()) { pr = pr * 10 + (fmt[i] - '0'); i++ }
                    precision = pr
                }
            }
            val lenStart = i
            while (i < fmt.length && fmt[i] in "hlLqjzt") i++
            val length = fmt.substring(lenStart, i)
            if (i >= fmt.length) fail(line, "printf: the format ends inside a % specifier")
            val conv = fmt[i]
            i++
            val spec = length + conv
            val f = flags.toString()
            when (conv) {
                'd', 'i' -> {
                    val v = integerArg(nextArg(spec), spec, line)
                    val sized = when (length) { "hh" -> v.toByte().toLong(); "h" -> v.toShort().toLong(); "" -> v.toInt().toLong(); else -> v }
                    out.append(pad(sized < 0, if (sized == Long.MIN_VALUE) "9223372036854775808" else kotlin.math.abs(sized).toString(), f, width, precision, ""))
                }
                'u', 'o', 'x', 'X' -> {
                    val v = integerArg(nextArg(spec), spec, line)
                    val masked = when (length) { "hh" -> v and 0xFF; "h" -> v and 0xFFFF; "" -> v and 0xFFFFFFFFL; else -> v }
                    val radix = when (conv) { 'o' -> 8; 'u' -> 10; else -> 16 }
                    var digits = java.lang.Long.toUnsignedString(masked, radix)
                    if (conv == 'X') digits = digits.uppercase()
                    val prefix = if ('#' in f && masked != 0L) when (conv) { 'x' -> "0x"; 'X' -> "0X"; 'o' -> "0"; else -> "" } else ""
                    out.append(pad(false, digits, f.replace("+", "").replace(" ", ""), width, precision, prefix))
                }
                'f', 'F', 'e', 'E', 'g', 'G' -> {
                    val v = floatArg(nextArg(spec), spec, line)
                    out.append(formatFloat(v, conv, f, width, precision))
                }
                'c' -> {
                    val v = integerArg(nextArg(spec), spec, line)
                    out.append(padText((v and 0xFFFF).toInt().toChar().toString(), f, width))
                }
                's' -> {
                    val (v, t) = nextArg(spec)
                    if (t !is CType.Ptr) fail(line, "printf: %s needs a string (char array or char *), but this is $t")
                    val ptr = v as CPtr
                    var text = if (ptr.obj == null) "(null)" else readString(ptr)
                    if (precision != null && text.length > precision) text = text.substring(0, precision)
                    out.append(padText(text, f, width))
                }
                'p' -> {
                    val (v, t) = nextArg(spec)
                    if (t !is CType.Ptr) fail(line, "printf: %p needs a pointer")
                    val ptr = v as CPtr
                    val text = if (ptr.obj == null) "(nil)" else "0x" + java.lang.Long.toHexString(0x7ff0_0000_0000L + ptr.obj.id * 0x10000L + ptr.index * 8L)
                    out.append(padText(text, f, width))
                }
                'n' -> fail(line, "printf: %n is not supported")
                else -> fail(line, "printf: unknown format %$spec")
            }
        }
        return out.toString()
    }

    private fun integerArg(arg: Pair<Any, CType>, spec: String, line: Int): Long {
        val (v, t) = arg
        if (!t.isInteger) {
            val hint = if (t.isFloating) " (use %f for float/double, or cast with (int))" else ""
            fail(line, "printf: %$spec needs a whole number, but this value is $t$hint")
        }
        return v as Long
    }

    private fun floatArg(arg: Pair<Any, CType>, spec: String, line: Int): Double {
        val (v, t) = arg
        if (!t.isFloating) {
            val hint = if (t.isInteger) " (use %d for int, or cast with (double))" else ""
            fail(line, "printf: %$spec needs a decimal number (float/double), but this value is $t$hint")
        }
        return v as Double
    }

    private fun formatFloat(v: Double, conv: Char, flags: String, width: Int, precision: Int?, ): String {
        val upper = conv.isUpperCase()
        val neg = v < 0 || (v == 0.0 && 1.0 / v < 0)
        if (v.isNaN() || v.isInfinite()) {
            val text = if (v.isNaN()) "nan" else "inf"
            return pad(neg && !v.isNaN(), if (upper) text.uppercase() else text, flags.replace("0", ""), width, null, "")
        }
        val a = kotlin.math.abs(v)
        val prec = precision ?: 6
        val alt = '#' in flags
        val body = when (conv.lowercaseChar()) {
            'f' -> fixed(a, prec, alt)
            'e' -> scientific(a, prec, alt, upper)
            else -> general(a, if (prec == 0) 1 else prec, alt, upper)
        }
        return pad(neg, body, flags, width, null, "")
    }

    private fun fixed(a: Double, prec: Int, alt: Boolean): String {
        val s = BigDecimal(a).setScale(prec, RoundingMode.HALF_EVEN).toPlainString()
        return if (prec == 0 && alt) "$s." else s
    }

    private fun scientific(a: Double, prec: Int, alt: Boolean, upper: Boolean): String {
        val (mantissa, exp) = sciParts(a, prec)
        val m = if (prec == 0 && alt) "$mantissa." else mantissa
        return m + (if (upper) "E" else "e") + (if (exp < 0) "-" else "+") + kotlin.math.abs(exp).toString().padStart(2, '0')
    }

    /** Mantissa text with [prec] decimals, and the decimal exponent. */
    private fun sciParts(a: Double, prec: Int): Pair<String, Int> {
        if (a == 0.0) return BigDecimal.ZERO.setScale(prec).toPlainString() to 0
        val rounded = BigDecimal(a).round(MathContext(prec + 1, RoundingMode.HALF_EVEN))
        val exp = rounded.precision() - rounded.scale() - 1
        val mantissa = rounded.movePointLeft(exp).setScale(prec, RoundingMode.HALF_EVEN).toPlainString()
        return mantissa to exp
    }

    private fun general(a: Double, p: Int, alt: Boolean, upper: Boolean): String {
        val x = if (a == 0.0) 0 else sciParts(a, p - 1).second
        var s = if (p > x && x >= -4) fixed(a, p - 1 - x, alt) else scientific(a, p - 1, alt, upper)
        if (!alt) {
            val ePos = s.indexOfFirst { it == 'e' || it == 'E' }
            val mant = if (ePos >= 0) s.substring(0, ePos) else s
            val expPart = if (ePos >= 0) s.substring(ePos) else ""
            val trimmed = if ('.' in mant) mant.trimEnd('0').trimEnd('.') else mant
            s = trimmed + expPart
        }
        return s
    }

    /** Sign, prefix, zero padding (`0` flag or precision) and width, as C does for numbers. */
    private fun pad(negative: Boolean, digits: String, flags: String, width: Int, precision: Int?, prefix: String): String {
        var d = digits
        if (precision != null) {
            d = if (precision == 0 && d == "0") "" else d.padStart(precision, '0')
        }
        val sign = when {
            negative -> "-"
            '+' in flags -> "+"
            ' ' in flags -> " "
            else -> ""
        }
        val left = '-' in flags
        val zero = '0' in flags && !left && precision == null
        return when {
            left -> (sign + prefix + d).padEnd(width)
            zero -> sign + prefix + d.padStart(width - sign.length - prefix.length, '0')
            else -> (sign + prefix + d).padStart(width)
        }
    }

    private fun padText(text: String, flags: String, width: Int) = if ('-' in flags) text.padEnd(width) else text.padStart(width)

    private fun fail(line: Int, reason: String): Nothing = throw CProgramException(line, reason, isCompile = false)
}
