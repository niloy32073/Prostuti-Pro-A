package com.prostuti.student.domain.code.c.run

/** C's arithmetic types; [rank] orders them for the usual arithmetic conversions. */
enum class NumKind(val bytes: Int, val rank: Int, val isFloat: Boolean, val cName: String) {
    BOOL(1, 0, false, "_Bool"),
    CHAR(1, 1, false, "char"),
    SHORT(2, 2, false, "short"),
    INT(4, 3, false, "int"),
    LONG(8, 4, false, "long"),
    LLONG(8, 5, false, "long long"),
    FLOAT(4, 6, true, "float"),
    DOUBLE(8, 7, true, "double"),
    LDOUBLE(16, 8, true, "long double"),
}
