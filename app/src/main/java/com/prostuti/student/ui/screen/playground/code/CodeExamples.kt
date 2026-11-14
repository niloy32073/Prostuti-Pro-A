package com.prostuti.student.ui.screen.playground.code

import com.prostuti.student.R
import com.prostuti.student.domain.code.CodeLanguage

/** HSC-level example programs, each in both languages. The first one is the starter code. */
object CodeExamples {

    fun starter(language: CodeLanguage): String = code(all.first(), language)

    fun code(example: CodeExample, language: CodeLanguage): String = when (language) {
        CodeLanguage.C -> example.c
        CodeLanguage.PYTHON -> example.python
    }

    val all: List<CodeExample> = listOf(
        CodeExample(
            R.string.code_ex_sum,
            c = """
                #include <stdio.h>

                int main() {
                    int n, sum = 0;
                    scanf("%d", &n);
                    for (int i = 1; i <= n; i++) {
                        sum = sum + i;
                    }
                    printf("Sum = %d\n", sum);
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                total = 0
                for i in range(1, n + 1):
                    total = total + i
                print("Sum =", total)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_even_odd,
            c = """
                #include <stdio.h>

                int main() {
                    int n;
                    scanf("%d", &n);
                    if (n % 2 == 0) {
                        printf("Even\n");
                    } else {
                        printf("Odd\n");
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                if n % 2 == 0:
                    print("Even")
                else:
                    print("Odd")
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_largest,
            c = """
                #include <stdio.h>

                int main() {
                    int a, b, c;
                    scanf("%d %d %d", &a, &b, &c);
                    if (a >= b && a >= c) {
                        printf("%d\n", a);
                    } else if (b >= c) {
                        printf("%d\n", b);
                    } else {
                        printf("%d\n", c);
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                a = int(input())
                b = int(input())
                c = int(input())
                if a >= b and a >= c:
                    print(a)
                elif b >= c:
                    print(b)
                else:
                    print(c)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_grade,
            c = """
                #include <stdio.h>

                int main() {
                    int marks;
                    scanf("%d", &marks);
                    if (marks >= 80) {
                        printf("A+\n");
                    } else if (marks >= 70) {
                        printf("A\n");
                    } else if (marks >= 60) {
                        printf("A-\n");
                    } else if (marks >= 33) {
                        printf("Pass\n");
                    } else {
                        printf("F\n");
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                marks = int(input())
                if marks >= 80:
                    print("A+")
                elif marks >= 70:
                    print("A")
                elif marks >= 60:
                    print("A-")
                elif marks >= 33:
                    print("Pass")
                else:
                    print("F")
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_factorial,
            c = """
                #include <stdio.h>

                int main() {
                    int n, i = 1;
                    long fact = 1;
                    scanf("%d", &n);
                    while (i <= n) {
                        fact = fact * i;
                        i++;
                    }
                    printf("%ld\n", fact);
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                fact = 1
                i = 1
                while i <= n:
                    fact = fact * i
                    i += 1
                print(fact)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_factorial_rec,
            c = """
                #include <stdio.h>

                long factorial(int n) {
                    if (n <= 1) {
                        return 1;
                    }
                    return n * factorial(n - 1);
                }

                int main() {
                    int n;
                    scanf("%d", &n);
                    long result = factorial(n);
                    printf("%ld\n", result);
                    return 0;
                }
            """.trimIndent(),
            python = """
                def factorial(n):
                    if n <= 1:
                        return 1
                    return n * factorial(n - 1)

                n = int(input())
                result = factorial(n)
                print(result)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_prime,
            c = """
                #include <stdio.h>

                int main() {
                    int n, isPrime = 1;
                    scanf("%d", &n);
                    if (n < 2) {
                        isPrime = 0;
                    }
                    for (int i = 2; i * i <= n; i++) {
                        if (n % i == 0) {
                            isPrime = 0;
                            break;
                        }
                    }
                    if (isPrime) {
                        printf("Prime\n");
                    } else {
                        printf("Not prime\n");
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                is_prime = n >= 2
                i = 2
                while i * i <= n:
                    if n % i == 0:
                        is_prime = False
                        break
                    i += 1
                if is_prime:
                    print("Prime")
                else:
                    print("Not prime")
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_gcd,
            c = """
                #include <stdio.h>

                int main() {
                    int a, b, r;
                    scanf("%d %d", &a, &b);
                    while (b != 0) {
                        r = a % b;
                        a = b;
                        b = r;
                    }
                    printf("GCD = %d\n", a);
                    return 0;
                }
            """.trimIndent(),
            python = """
                a = int(input())
                b = int(input())
                while b != 0:
                    a, b = b, a % b
                print("GCD =", a)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_fibonacci,
            c = """
                #include <stdio.h>

                int main() {
                    int n, a = 0, b = 1, next;
                    scanf("%d", &n);
                    for (int i = 1; i <= n; i++) {
                        printf("%d ", a);
                        next = a + b;
                        a = b;
                        b = next;
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                a, b = 0, 1
                for i in range(n):
                    print(a, end=" ")
                    a, b = b, a + b
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_reverse,
            c = """
                #include <stdio.h>

                int main() {
                    int n, rev = 0;
                    scanf("%d", &n);
                    do {
                        rev = rev * 10 + n % 10;
                        n = n / 10;
                    } while (n > 0);
                    printf("%d\n", rev);
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                rev = 0
                while n > 0:
                    rev = rev * 10 + n % 10
                    n = n // 10
                print(rev)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_calculator,
            c = """
                #include <stdio.h>

                int main() {
                    float a, b;
                    char op;
                    scanf("%f %c %f", &a, &op, &b);
                    switch (op) {
                        case '+': printf("%.2f\n", a + b); break;
                        case '-': printf("%.2f\n", a - b); break;
                        case '*': printf("%.2f\n", a * b); break;
                        case '/':
                            if (b != 0) printf("%.2f\n", a / b);
                            else printf("Cannot divide by 0\n");
                            break;
                        default: printf("Unknown operator\n");
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                a = float(input())
                op = input()
                b = float(input())
                match op:
                    case "+":
                        print(a + b)
                    case "-":
                        print(a - b)
                    case "*":
                        print(a * b)
                    case "/":
                        if b != 0:
                            print(a / b)
                        else:
                            print("Cannot divide by 0")
                    case _:
                        print("Unknown operator")
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_bubble,
            c = """
                #include <stdio.h>

                int main() {
                    int a[5] = {5, 1, 4, 2, 8};
                    int n = 5, temp;
                    for (int i = 0; i < n - 1; i++) {
                        for (int j = 0; j < n - 1 - i; j++) {
                            if (a[j] > a[j + 1]) {
                                temp = a[j];
                                a[j] = a[j + 1];
                                a[j + 1] = temp;
                            }
                        }
                    }
                    for (int i = 0; i < n; i++) printf("%d ", a[i]);
                    return 0;
                }
            """.trimIndent(),
            python = """
                a = [5, 1, 4, 2, 8]
                n = len(a)
                for i in range(n - 1):
                    for j in range(n - 1 - i):
                        if a[j] > a[j + 1]:
                            a[j], a[j + 1] = a[j + 1], a[j]
                print(a)
            """.trimIndent(),
        ),
        CodeExample(
            R.string.code_ex_pattern,
            c = """
                #include <stdio.h>

                int main() {
                    int n;
                    scanf("%d", &n);
                    for (int i = 1; i <= n; i++) {
                        for (int j = 1; j <= i; j++) {
                            printf("* ");
                        }
                        printf("\n");
                    }
                    return 0;
                }
            """.trimIndent(),
            python = """
                n = int(input())
                for i in range(1, n + 1):
                    for j in range(i):
                        print("*", end=" ")
                    print()
            """.trimIndent(),
        ),
    )
}
