package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.c.run.CType.Companion.CHAR_PTR
import com.prostuti.student.domain.code.c.run.CType.Companion.DOUBLE
import com.prostuti.student.domain.code.c.run.CType.Companion.INT
import com.prostuti.student.domain.code.c.run.CType.Companion.LONG
import com.prostuti.student.domain.code.c.run.CType.Companion.UINT
import com.prostuti.student.domain.code.c.run.CType.Companion.ULONG
import com.prostuti.student.domain.code.c.run.CType.Companion.VOID_PTR

/**
 * The library functions a student program may call (stdio, math, stdlib, string, ctype, conio).
 * Streams (`stdin`, `stdout`, `stderr`) are passed as [STREAM] numbers. [variadic] functions take
 * any further arguments after [params].
 */
enum class Builtin(val cName: String, val ret: CType, val params: List<CType>, val variadic: Boolean = false) {
    PRINTF("printf", INT, listOf(CHAR_PTR), true),
    SCANF("scanf", INT, listOf(CHAR_PTR), true),
    SPRINTF("sprintf", INT, listOf(CHAR_PTR, CHAR_PTR), true),
    FPRINTF("fprintf", INT, listOf(LONG, CHAR_PTR), true),
    PUTS("puts", INT, listOf(CHAR_PTR)),
    PUTCHAR("putchar", INT, listOf(INT)),
    GETCHAR("getchar", INT, emptyList()),
    GETS("gets", CHAR_PTR, listOf(CHAR_PTR)),
    FGETS("fgets", CHAR_PTR, listOf(CHAR_PTR, INT, LONG)),
    GETCH("getch", INT, emptyList()),
    GETCHE("getche", INT, emptyList()),
    FFLUSH("fflush", INT, listOf(LONG)),
    CLRSCR("clrscr", CType.Void, emptyList()),

    SQRT("sqrt", DOUBLE, listOf(DOUBLE)),
    CBRT("cbrt", DOUBLE, listOf(DOUBLE)),
    POW("pow", DOUBLE, listOf(DOUBLE, DOUBLE)),
    FABS("fabs", DOUBLE, listOf(DOUBLE)),
    FLOOR("floor", DOUBLE, listOf(DOUBLE)),
    CEIL("ceil", DOUBLE, listOf(DOUBLE)),
    ROUND("round", DOUBLE, listOf(DOUBLE)),
    TRUNC("trunc", DOUBLE, listOf(DOUBLE)),
    SIN("sin", DOUBLE, listOf(DOUBLE)),
    COS("cos", DOUBLE, listOf(DOUBLE)),
    TAN("tan", DOUBLE, listOf(DOUBLE)),
    ASIN("asin", DOUBLE, listOf(DOUBLE)),
    ACOS("acos", DOUBLE, listOf(DOUBLE)),
    ATAN("atan", DOUBLE, listOf(DOUBLE)),
    ATAN2("atan2", DOUBLE, listOf(DOUBLE, DOUBLE)),
    EXP("exp", DOUBLE, listOf(DOUBLE)),
    LOG("log", DOUBLE, listOf(DOUBLE)),
    LOG10("log10", DOUBLE, listOf(DOUBLE)),
    LOG2("log2", DOUBLE, listOf(DOUBLE)),
    FMOD("fmod", DOUBLE, listOf(DOUBLE, DOUBLE)),
    HYPOT("hypot", DOUBLE, listOf(DOUBLE, DOUBLE)),

    ABS("abs", INT, listOf(INT)),
    LABS("labs", LONG, listOf(LONG)),
    RAND("rand", INT, emptyList()),
    SRAND("srand", CType.Void, listOf(UINT)),
    TIME("time", LONG, listOf(VOID_PTR)),
    EXIT("exit", CType.Void, listOf(INT)),
    MALLOC("malloc", VOID_PTR, listOf(ULONG)),
    CALLOC("calloc", VOID_PTR, listOf(ULONG, ULONG)),
    FREE("free", CType.Void, listOf(VOID_PTR)),
    ATOI("atoi", INT, listOf(CHAR_PTR)),
    ATOF("atof", DOUBLE, listOf(CHAR_PTR)),

    STRLEN("strlen", ULONG, listOf(CHAR_PTR)),
    STRCPY("strcpy", CHAR_PTR, listOf(CHAR_PTR, CHAR_PTR)),
    STRNCPY("strncpy", CHAR_PTR, listOf(CHAR_PTR, CHAR_PTR, ULONG)),
    STRCAT("strcat", CHAR_PTR, listOf(CHAR_PTR, CHAR_PTR)),
    STRNCAT("strncat", CHAR_PTR, listOf(CHAR_PTR, CHAR_PTR, ULONG)),
    STRCMP("strcmp", INT, listOf(CHAR_PTR, CHAR_PTR)),
    STRNCMP("strncmp", INT, listOf(CHAR_PTR, CHAR_PTR, ULONG)),
    STRREV("strrev", CHAR_PTR, listOf(CHAR_PTR)),
    STRUPR("strupr", CHAR_PTR, listOf(CHAR_PTR)),
    STRLWR("strlwr", CHAR_PTR, listOf(CHAR_PTR)),

    ISDIGIT("isdigit", INT, listOf(INT)),
    ISALPHA("isalpha", INT, listOf(INT)),
    ISALNUM("isalnum", INT, listOf(INT)),
    ISUPPER("isupper", INT, listOf(INT)),
    ISLOWER("islower", INT, listOf(INT)),
    ISSPACE("isspace", INT, listOf(INT)),
    ISPUNCT("ispunct", INT, listOf(INT)),
    TOUPPER("toupper", INT, listOf(INT)),
    TOLOWER("tolower", INT, listOf(INT)),
    ;

    companion object {
        private val byName = entries.associateBy { it.cName }
        fun find(name: String): Builtin? = byName[name]

        /** `stdin`, `stdout`, `stderr` as stream numbers. */
        val STREAMS = mapOf("stdin" to 0L, "stdout" to 1L, "stderr" to 2L)
    }
}
