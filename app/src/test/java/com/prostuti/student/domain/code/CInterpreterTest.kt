package com.prostuti.student.domain.code

import com.prostuti.student.domain.code.c.run.CCompiler
import com.prostuti.student.domain.code.c.run.CInterpreter
import com.prostuti.student.domain.code.c.run.CIo
import com.prostuti.student.domain.code.c.run.CProgramException
import com.prostuti.student.ui.screen.playground.code.CodeExamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs C programs through the interpreter and compares their output with what gcc prints. */
class CInterpreterTest {

    private class Result(val out: String, val error: CProgramException?)

    private fun run(code: String, vararg input: String): Result {
        val lines = ArrayDeque(input.toList())
        val out = StringBuilder()
        var error: CProgramException? = null
        val io = object : CIo {
            override fun write(text: String) { out.append(text) }
            override fun readLine(): String? = lines.removeFirstOrNull()
            override val stopped = false
        }
        // A big stack, as the app's runner thread has.
        val t = Thread(null, {
            try {
                CInterpreter(io).run(CCompiler.compile(code.trimIndent()))
            } catch (e: CProgramException) {
                error = e
            }
        }, "c", 32L * 1024 * 1024)
        t.start()
        t.join()
        return Result(out.toString(), error)
    }

    private fun output(code: String, vararg input: String): String {
        val r = run(code, *input)
        assertNull(r.error?.report(), r.error)
        return r.out
    }

    private fun error(code: String, vararg input: String): CProgramException {
        val r = run(code, *input)
        return r.error ?: throw AssertionError("expected an error, output was: ${r.out}")
    }

    private fun main(body: String) = "#include <stdio.h>\nint main() {\n$body\nreturn 0;\n}"

    @Test fun printfFormatsLikeGlibc() {
        assertEquals(
            "42|  42|42  |0042|+42|-7|ff|FF|0x1f|17|3.142|3.14159|  3.1|1.500000e+02|1.5E-05|0.0001|1e+06|123456|1.23457e+06|A|hi  |  hi|%|4294967295\n",
            output(
                main(
                    """
                    printf("%d|%4d|%-4d|%04d|%+d|%i|%x|%X|%#x|%o|", 42, 42, 42, 42, 42, -7, 255, 255, 31, 15);
                    printf("%.3f|%g|%5.1f|%e|%.1E|%g|%g|%g|%g|", 3.14159, 3.14159, 3.14159, 150.0, 0.000015, 0.0001, 1000000.0, 123456.0, 1234567.0);
                    printf("%c|%-4s|%4s|%%|%u\n", 65, "hi", "hi", -1);
                    """
                ),
            ),
        )
        assertEquals("0.12 0.38 2 -0.000000 inf\n", output(main("""printf("%.2f %.2f %.0f %f %f\n", 0.125, 0.375, 2.5, -0.0, 1.0/0.0);""")))
        assertEquals("[   7] [7   ] [007]\n", output(main("""printf("[%*d] [%-*d] [%.3d]\n", 4, 7, 4, 7, 7);""")))
    }

    @Test fun printfWithTheWrongKindOfValueIsAnError() {
        val e = error(main("float x = 2.5;\nprintf(\"%d\", x);"))
        assertEquals(4, e.line)
        assertTrue(e.reason, e.reason.contains("%d") && e.reason.contains("%f"))
        assertTrue(error(main("int n = 3;\nprintf(\"%f\", n);")).reason.contains("%d"))
        assertTrue(error(main("printf(\"%d %d\", 1);")).reason.contains("more %"))
    }

    @Test fun scanfReadsAcrossLinesAndChecksTypes() {
        val code = main(
            """
            int a, b; float f; double d; char c; char name[20];
            scanf("%d %d", &a, &b);
            scanf("%f%lf", &f, &d);
            scanf(" %c", &c);
            scanf("%s", name);
            printf("%d %d %.2f %.3lf %c %s\n", a + b, a * b, f, d, c, name);
            """
        )
        assertEquals("7 12 1.50 2.250 x Rahim\n", output(code, "3", "4 1.5", "2.25", "  x Rahim"))
        assertTrue(error(main("double d;\nscanf(\"%f\", &d);"), "1").reason.contains("%lf"))
        assertTrue(error(main("int n;\nscanf(\"%d\", n);")).let { it.isCompile && it.reason.contains("&n") })
        assertEquals("-1 0\n", output(main("int n = 0; int r = scanf(\"%d\", &n);\nprintf(\"%d %d\\n\", r, n);")))
        assertEquals("0 5\n", output(main("int n = 5; int r = scanf(\"%d\", &n);\nprintf(\"%d %d\\n\", r, n);"), "abc"))
    }

    @Test fun linesWithSpacesGetsAndCharByChar() {
        val code = main(
            """
            char line[50]; int n; char c;
            scanf("%d", &n);
            getchar();
            fgets(line, 50, stdin);
            c = getchar();
            printf("[%d][%s][%c]\n", n, line, c);
            """
        )
        assertEquals("[3][hello world\n][Z]\n", output(code, "3", "hello world", "Z"))
        assertEquals("[Karim Ali]\n", output(main("char s[30];\nscanf(\"%[^\\n]\", s);\nprintf(\"[%s]\\n\", s);"), "Karim Ali"))
    }

    @Test fun integerArithmeticFollowsC() {
        assertEquals(
            "3 -3 1 -1 2 2.500000 -2147483648 255 -1 4294967295 1\n",
            output(main("""int big = 2147483647; unsigned char uc = 255; char sc = 255; unsigned int u = 0;
                printf("%d %d %d %d %d %f %d %d %d %u %d\n", 7/2, -7/2, 7%3, -7%3, 5/2, 5/2.0, big + 1, uc, sc, u - 1, 3 > 2);""")),
        )
        assertEquals("1 0 6 7 8 2 -1\n", output(main("""printf("%d %d %d %d %d %d %d\n", 5 & 3, 5 & 2, 5 ^ 3, 5 | 2, 1 << 3, 8 >> 2, ~0);""")))
        assertEquals("2432902008176640000\n", output(main("long f = 1;\nfor (int i = 1; i <= 20; i++) f *= i;\nprintf(\"%ld\\n\", f);")))
        assertEquals("3 3.33\n", output(main("int a = 10, b = 3;\nfloat r = (float)a / b;\nprintf(\"%d %.2f\\n\", a / b, r);")))
        assertEquals("97 b 3\n", output(main("char c = 'a';\nprintf(\"%d %c %d\\n\", c, c + 1, '3' - '0');")))
    }

    @Test fun controlFlow() {
        val code = main(
            """
            int s = 0;
            for (int i = 1; i <= 10; i++) { if (i == 3) continue; if (i == 8) break; s += i; }
            int j = 0; while (1) { j++; if (j > 4) break; }
            int k = 10; do { k -= 3; } while (k > 0);
            int g = 2; switch (g) { case 1: printf("one "); case 2: printf("two "); case 3: printf("three "); break; default: printf("other "); }
            switch (9) { case 1: printf("x"); default: printf("def "); }
            printf("%d %d %d %d\n", s, j, k, s > 20 ? 1 : 0);
            """
        )
        assertEquals("two three def 25 5 -2 1\n", output(code))
    }

    @Test fun arraysStringsAndPointers() {
        val code = """
            #include <stdio.h>
            #include <string.h>
            void swap(int *a, int *b) { int t = *a; *a = *b; *b = t; }
            int sum(int arr[], int n) { int s = 0; for (int i = 0; i < n; i++) s += arr[i]; return s; }
            int main() {
                int a[5] = {5, 1, 4};
                int m[2][3] = {{1, 2, 3}, {4, 5, 6}};
                int flat[2][2] = {1, 2, 3};
                char s[20] = "Dhaka";
                char t[20];
                int x = 1, y = 2, *p = a;
                swap(&x, &y);
                strcpy(t, s);
                strcat(t, " City");
                *(p + 3) = 9;
                p++;
                printf("%d %d %d %d %d\n", sum(a, 5), m[1][2], flat[1][0], flat[1][1], *p);
                printf("%d %d %s %lu %d %s\n", x, y, t, strlen(t), strcmp("abc", "abd"), strupr(s));
                char *q = "hello";
                int len = 0; while (q[len] != '\0') len++;
                printf("%d %c %ld %lu %lu\n", len, *(q + 1), &a[4] - &a[1], sizeof(a), sizeof(double));
                return 0;
            }
        """
        assertEquals("19 6 3 0 1\n2 1 Dhaka City 10 -1 DHAKA\n5 e 3 20 8\n", output(code))
    }

    @Test fun variableLengthArraysAndMalloc() {
        val code = main(
            """
            int n; scanf("%d", &n);
            int a[n];
            for (int i = 0; i < n; i++) a[i] = i * i;
            int *b = (int *) malloc(n * sizeof(int));
            for (int i = 0; i < n; i++) b[i] = a[i] + 1;
            printf("%d %d\n", a[n - 1], b[n - 1]);
            free(b);
            """
        )
        assertEquals("16 17\n", output(code, "5"))
    }

    @Test fun recursionGlobalsStaticAndMacros() {
        val code = """
            #include <stdio.h>
            #define MAX 5
            #define SQR(x) ((x) * (x))
            int count = 0;
            int fib(int n) { count++; return n < 2 ? n : fib(n - 1) + fib(n - 2); }
            int next() { static int k = 0; return ++k; }
            int main() {
                printf("%d %d %d ", fib(10), MAX, SQR(MAX + 1));
                next(); next();
                printf("%d %d\n", next(), count);
                return 0;
            }
        """
        assertEquals("55 5 36 3 177\n", output(code))
    }

    @Test fun mathLibrary() {
        assertEquals(
            "4.00 8.00 3.00 4.00 1.00 3.14 2\n",
            output("#include <stdio.h>\n#include <math.h>\nint main() { printf(\"%.2f %.2f %.2f %.2f %.2f %.2f %d\\n\", sqrt(16), pow(2, 3), floor(3.7), ceil(3.2), sin(M_PI / 2), M_PI, abs(-2)); return 0; }"),
        )
    }

    @Test fun snippetWithoutMainRuns() {
        assertEquals("15\n", output("int s = 0;\nfor (int i = 1; i <= 5; i++) s += i;\nprintf(\"%d\\n\", s);"))
    }

    @Test fun runtimeErrorsPointAtTheLine() {
        assertEquals(4, error(main("int a = 5, b = 0;\nint c = a / b;")).line)
        val oob = error(main("int a[3];\nfor (int i = 0; i <= 3; i++) a[i] = i;"))
        assertEquals(4, oob.line)
        assertTrue(oob.reason, oob.reason.contains("index 3") && oob.reason.contains("0 to 2"))
        val uninit = error(main("int sum;\nfor (int i = 0; i < 3; i++) sum += i;"))
        assertTrue(uninit.reason, uninit.reason.contains("'sum'"))
        assertTrue(error(main("int *p = NULL;\n*p = 3;")).reason.contains("NULL"))
        assertTrue(error(main("char *s = \"hi\";\ns[0] = 'H';")).reason.contains("string literal"))
        assertTrue(error("int f(int n) { return f(n + 1); }\nint main() { return f(0); }").reason.contains("f"))
        assertTrue(error(main("char s[4];\nstrcpy(s, \"hello\");")).reason.contains("room"))
    }

    @Test fun compileErrors() {
        fun compileError(code: String): CProgramException = error(code).also { assertTrue(it.reason, it.isCompile) }
        assertEquals(3, compileError(main("x = 5;")).line)
        assertTrue(compileError(main("int a = 1\nprintf(\"%d\", a);")).reason.contains(";"))
        assertTrue(compileError("int main() { foo(1); return 0; }").reason.contains("foo"))
        assertTrue(compileError("int add(int a, int b) { return a + b; }\nint main() { return add(1); }").reason.contains("2 argument"))
        assertTrue(compileError("struct P { int x : 3; };\nint main() { return 0; }").reason.contains("bit-field"))
        assertTrue(compileError("int main() { int a[3]; int b[3]; a = b; return 0; }").reason.contains("array"))
        assertTrue(compileError("void f() {}\nint main() { int x = f(); return 0; }").reason.contains("void"))
        assertTrue(compileError("int g() { return 1; }").reason.contains("main"))
    }

    @Test fun everyCExampleRunsWithTheExpectedOutput() {
        val expected = mapOf(
            com.prostuti.student.R.string.code_ex_sum to ("5" to "Sum = 15\n"),
            com.prostuti.student.R.string.code_ex_even_odd to ("7" to "Odd\n"),
            com.prostuti.student.R.string.code_ex_largest to ("3 9 4" to "9\n"),
            com.prostuti.student.R.string.code_ex_grade to ("72" to "A\n"),
            com.prostuti.student.R.string.code_ex_factorial to ("5" to "120\n"),
            com.prostuti.student.R.string.code_ex_factorial_rec to ("6" to "720\n"),
            com.prostuti.student.R.string.code_ex_prime to ("29" to "Prime\n"),
            com.prostuti.student.R.string.code_ex_gcd to ("12 18" to "GCD = 6\n"),
            com.prostuti.student.R.string.code_ex_fibonacci to ("7" to "0 1 1 2 3 5 8 "),
            com.prostuti.student.R.string.code_ex_reverse to ("1234" to "4321\n"),
            com.prostuti.student.R.string.code_ex_calculator to ("6 * 7" to "42.00\n"),
            com.prostuti.student.R.string.code_ex_bubble to ("" to "1 2 4 5 8 "),
            com.prostuti.student.R.string.code_ex_pattern to ("3" to "* \n* * \n* * * \n"),
        )
        assertEquals(CodeExamples.all.size, expected.size)
        for (example in CodeExamples.all) {
            val (input, out) = expected.getValue(example.titleRes)
            assertEquals("example ${example.titleRes}", out, output(example.c, *input.split("\n").filter { it.isNotEmpty() }.toTypedArray()))
        }
    }

    @Test fun structsTypedefsAndLinkedLists() {
        // Expected output from clang.
        val code = """
            #include <stdio.h>
            #include <string.h>
            #include <stdlib.h>
            struct Point { int x; int y; };
            typedef struct {
                char name[20];
                int roll;
                float marks[3];
            } Student;
            struct Node { int value; struct Node *next; };
            struct Point make(int x, int y) { struct Point p = { x, y }; return p; }
            void shift(struct Point *p, int d) { p->x += d; (*p).y += d; }
            float average(Student s) { return (s.marks[0] + s.marks[1] + s.marks[2]) / 3; }
            int main() {
                struct Point a = { 1, 2 }, b;
                b = a;
                b.x = 10;
                shift(&a, 5);
                printf("%d %d %d %d\n", a.x, a.y, b.x, b.y);
                struct Point c = make(3, 4);
                printf("%d %d %d\n", c.x, c.y, make(7, 8).y);
                Student list[2] = { { "Rahim", 1, { 80, 90, 70 } }, { .roll = 2, .name = "Karim" } };
                list[1].marks[0] = 60; list[1].marks[1] = 70; list[1].marks[2] = 80;
                for (int i = 0; i < 2; i++) printf("%s %d %.2f\n", list[i].name, list[i].roll, average(list[i]));
                Student *sp = &list[1];
                strcpy(sp->name, "Jamal");
                printf("%s %c %lu %lu\n", list[1].name, sp->name[0], sizeof(struct Point), sizeof(Student));
                struct Point grid[2][2] = { 1, 2, 3, 4, 5, 6, 7, 8 };
                printf("%d %d\n", grid[1][0].x, grid[0][1].y);
                struct Node *head = NULL;
                for (int i = 1; i <= 3; i++) {
                    struct Node *n = malloc(sizeof(struct Node));
                    n->value = i * 10;
                    n->next = head;
                    head = n;
                }
                for (struct Node *p = head; p != NULL; p = p->next) printf("%d ", p->value);
                printf("\n");
                static struct Point zero;
                printf("%d %d\n", zero.x, zero.y);
                return 0;
            }
        """
        assertEquals(
            "6 7 10 2\n3 4 8\nRahim 1 80.00\nKarim 2 70.00\nJamal J 8 36\n5 4\n30 20 10 \n0 0\n",
            output(code),
        )
    }

    @Test fun enumsUnionsFunctionPointersGotoAndConditionalCompilation() {
        // Expected output from clang.
        val code = """
            #include <stdio.h>
            #define LEVEL 2
            #define DEBUG
            enum Color { RED, GREEN = 5, BLUE };
            typedef enum { SMALL = 1, BIG = SMALL * 10 } Size;
            typedef int number;
            typedef unsigned long ulong;
            union Value { int i; double d; char c; };
            int add(int a, int b) { return a + b; }
            int sub(int a, int b) { return a - b; }
            int mul(int a, int b) { return a * b; }
            int apply(int (*f)(int, int), int x, int y) { return f(x, y); }
            typedef int (*Op)(int, int);
            int (*pick(char c))(int, int) { return c == '+' ? add : sub; }
            int main() {
                enum Color c = BLUE;
                Size s = BIG;
                number n = 7;
                ulong big = 4000000000UL;
                printf("%d %d %d %d %d %lu\n", RED, GREEN, c, s, n, big);
                switch (c) { case RED: printf("red\n"); break; case BLUE: printf("blue\n"); break; default: printf("?\n"); }
                union Value v;
                v.i = 65;
                printf("%d %c\n", v.i, v.c);
                v.d = 2.5;
                printf("%.1f %lu\n", v.d, sizeof(union Value));
                int (*op)(int, int) = add;
                printf("%d %d %d\n", op(2, 3), (*op)(2, 3), apply(mul, 4, 5));
                Op table[3] = { add, sub, &mul };
                for (int i = 0; i < 3; i++) printf("%d ", table[i](10, 2));
                printf("%d\n", pick('-')(9, 4));
                printf("%d\n", op == add);
                int i = 0;
            again:
                i++;
                if (i < 3) goto again;
                printf("i=%d\n", i);
                for (int r = 0; r < 3; r++)
                    for (int k = 0; k < 3; k++)
                        if (r * k == 2) { printf("found %d %d\n", r, k); goto done; }
            done:
            #if LEVEL > 1 && defined(DEBUG)
                printf("level>1\n");
            #elif LEVEL == 1
                printf("level1\n");
            #else
                printf("other\n");
            #endif
            #ifndef DEBUG
                printf("nodebug\n");
            #endif
            #ifdef MISSING
                printf("missing\n");
            #else
                printf("no missing\n");
            #endif
                return 0;
            }
        """
        assertEquals(
            "0 5 6 10 7 4000000000\nblue\n65 A\n2.5 8\n5 5 20\n12 8 20 5\n1\ni=3\nfound 1 2\nlevel>1\nno missing\n",
            output(code),
        )
    }

    @Test fun nestedStructsUnionsCallbacksAndPointersToArrays() {
        // Expected output from clang.
        val code = """
            #include <stdio.h>
            #include <string.h>
            typedef struct { int day, month, year; } Date;
            typedef struct { char name[30]; Date dob; float gpa; } Student;
            typedef struct Shape { int kind; union { double radius; struct { double w, h; } rect; } u; double (*area)(struct Shape *); } Shape;
            double circleArea(Shape *s) { return 3.14 * s->u.radius * s->u.radius; }
            double rectArea(Shape *s) { return s->u.rect.w * s->u.rect.h; }
            void sortByGpa(Student a[], int n) {
                for (int i = 0; i < n - 1; i++)
                    for (int j = 0; j < n - 1 - i; j++)
                        if (a[j].gpa < a[j + 1].gpa) { Student t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; }
            }
            int cmpAsc(int a, int b) { return a - b; }
            int cmpDesc(int a, int b) { return b - a; }
            void sortInts(int *v, int n, int (*cmp)(int, int)) {
                for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) if (cmp(v[i], v[j]) > 0) { int t = v[i]; v[i] = v[j]; v[j] = t; }
            }
            typedef int *IntPtr;
            enum Day { SAT = 6, SUN = 0, MON };
            int main() {
                Student s[3] = {
                    { "Ana", { 1, 2, 2005 }, 3.5f },
                    { "Bob", .dob = { 3, 4, 2004 }, .gpa = 3.9f },
                    { .name = "Cid", .gpa = 3.7f }
                };
                sortByGpa(s, 3);
                for (int i = 0; i < 3; i++) printf("%-4s %02d/%02d/%d %.1f\n", s[i].name, s[i].dob.day, s[i].dob.month, s[i].dob.year, s[i].gpa);
                Shape shapes[2];
                shapes[0].kind = 0; shapes[0].u.radius = 2; shapes[0].area = circleArea;
                shapes[1].kind = 1; shapes[1].u.rect.w = 3; shapes[1].u.rect.h = 4; shapes[1].area = &rectArea;
                for (int i = 0; i < 2; i++) printf("%.2f\n", shapes[i].area(&shapes[i]));
                int v[5] = { 4, 1, 5, 2, 3 };
                sortInts(v, 5, cmpDesc);
                for (int i = 0; i < 5; i++) printf("%d", v[i]);
                sortInts(v, 5, cmpAsc);
                printf(" ");
                for (int i = 0; i < 5; i++) printf("%d", v[i]);
                printf("\n");
                int m[2][3] = { { 1, 2, 3 }, { 4, 5, 6 } };
                int (*row)[3] = m;
                row++;
                printf("%d %d %lu\n", (*row)[1], row[0][2], sizeof(*row));
                IntPtr ip = &v[2];
                *ip = 99;
                printf("%d %d %d\n", v[2], MON, sizeof(Shape) > 0);
                enum Day d = MON;
                switch (d) { case SAT: puts("sat"); break; case MON: puts("mon"); break; default: break; }
                struct { int a; char b; } anon = { 7, 'z' };
                printf("%d %c %lu\n", anon.a, anon.b, sizeof(anon));
                Date *dp = &s[0].dob;
                dp->year++;
                printf("%d\n", s[0].dob.year);
                char buf[10];
                int count = 0;
            loop:
                if (count < 3) { buf[count] = 'a' + count; count++; goto loop; }
                buf[count] = '\0';
                printf("%s\n", buf);
            #if (2 + 3) * 2 == 10 && !defined(X)
                puts("math ok");
            #endif
                return 0;
            }
        """
        assertEquals("Bob  03/04/2004 3.9\nCid  00/00/0 3.7\nAna  01/02/2005 3.5\n12.56\n12.00\n54321 12345\n5 6 12\n99 1 1\nmon\n7 z 8\n2005\nabc\nmath ok\n", output(code))
    }

    @Test fun structRuntimeErrorsNameTheMember() {
        val decl = "struct P { int x; int y; };\ntypedef struct { char name[8]; int marks[3]; } S;\nunion U { int i; double d; };\n"
        fun run(body: String) = error(decl + "int main() {\n$body\nreturn 0;\n}")
        assertTrue(run("struct P p;\np.x = 1;\nprintf(\"%d\", p.y);").reason.contains("'p.y'"))
        assertTrue(run("struct P a[3];\na[2].x = 1;\nprintf(\"%d\", a[2].y);").reason.contains("'a[2].y'"))
        val bound = run("S s;\ns.marks[3] = 1;")
        assertTrue(bound.reason, bound.reason.contains("index 3") && bound.reason.contains("s.marks"))
        assertEquals(6, bound.line)
        assertTrue(run("union U u;\nu.d = 2.5;\nprintf(\"%d\", u.i);").reason.contains("union member 'i'"))
        assertTrue(run("struct P *p = NULL;\np->x = 1;").reason.contains("NULL"))
        assertTrue(run("int (*f)(int) = NULL;\nf(1);").reason.contains("NULL function pointer"))
        assertTrue(run("int m[2][3];\nm[0][3] = 1;").reason.contains("index 3"))
    }

    @Test fun structAndGotoCompileErrors() {
        fun compileError(code: String): CProgramException = error(code).also { assertTrue(it.reason, it.isCompile) }
        val decl = "struct P { int x; int y; };\n"
        fun inMain(body: String) = compileError(decl + "int main() {\n$body\nreturn 0;\n}")
        assertTrue(inMain("struct P p;\np.z = 1;").reason.contains("no member 'z'"))
        assertTrue(inMain("struct P p, *q = &p;\nq.x = 1;").reason.contains("'->'"))
        assertTrue(inMain("struct P p;\np->x = 1;").reason.contains("'.'"))
        assertTrue(inMain("struct P p = {1, 2};\nprintf(\"%d\", p);").reason.contains("members"))
        assertTrue(inMain("struct Q q;").reason.contains("not complete"))
        assertTrue(compileError("struct A { int x; struct A inner; };\nint main() { return 0; }").reason.contains("pointer"))
        assertTrue(inMain("goto nowhere;").reason.contains("nowhere"))
        assertTrue(inMain("goto in;\nfor (int i = 0; i < 2; i++) { in: i++; }").reason.contains("into"))
        assertTrue(inMain("int (*f)(int, int) = printf;").reason.isNotEmpty())
        assertTrue(compileError("double half(double x) { return x / 2; }\nint main() { int (*f)(int) = half; return 0; }").reason.contains("type"))
        assertTrue(inMain("struct P p = {1, 2, 3};").reason.contains("too many"))
        assertTrue(compileError("#if 1\nint main() { return 0; }").reason.contains("#endif"))
        val err = compileError("#ifndef N\n#error N must be defined\n#endif\nint main() { return 0; }")
        assertTrue(err.reason, err.reason.contains("N must be defined") && err.line == 2)
    }

    @Test fun turboCStyleProgramsWork() {
        val code = """
            #include <stdio.h>
            #include <conio.h>
            void main() {
                int i;
                clrscr();
                for (i = 0; i < 3; i++) printf("%d", i);
                getch();
            }
        """
        assertEquals("012", output(code, ""))
    }
}
