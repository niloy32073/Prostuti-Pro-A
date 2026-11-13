package com.prostuti.student.ui.screen.playground.graph.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import java.io.File

/** Builds the shared picture (graph on top, the coloured equation list below) and opens the share sheet. */
object GraphShareImage {

    fun compose(
        graph: Bitmap,
        rows: List<ShareRow>,
        footer: String,
        background: Int,
        ink: Int,
        muted: Int,
        density: Float,
    ): Bitmap {
        val width = graph.width
        val pad = 16 * density
        val dot = 7 * density
        val textX = pad + dot * 2 + 10 * density
        val textWidth = (width - textX - pad).toInt().coerceAtLeast(1)
        val main = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; textSize = 17 * density; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val detail = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 14 * density; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
        val note = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 15 * density; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }
        fun layout(text: String, paint: TextPaint) =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        val blocks = rows.map { row ->
            val first = layout(row.text, if (row.isNote) note else main)
            val second = row.detail?.let { layout(it, detail) }
            Triple(row, first, second)
        }
        val listHeight = blocks.sumOf { (_, a, b) -> (a.height + (b?.height ?: 0) + 12 * density).toDouble() }.toFloat()
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 12 * density }
        val height = (graph.height + pad * 2 + listHeight + 24 * density).toInt()
        val out = createBitmap(width, height)
        val canvas = Canvas(out)
        canvas.drawColor(background)
        canvas.drawBitmap(graph, 0f, 0f, null)
        canvas.drawLine(0f, graph.height.toFloat(), width.toFloat(), graph.height.toFloat(), Paint().apply { color = muted; alpha = 70 })
        var y = graph.height + pad
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for ((row, first, second) in blocks) {
            if (!row.isNote) {
                dotPaint.color = row.color
                canvas.drawCircle(pad + dot, y + first.getLineBaseline(0) - main.textSize * 0.35f, dot, dotPaint)
            }
            canvas.withTranslation(textX, y) { first.draw(this) }
            y += first.height
            if (second != null) {
                canvas.withTranslation(textX, y) { second.draw(this) }
                y += second.height
            }
            y += 12 * density
        }
        canvas.drawText(footer, width - pad - footerPaint.measureText(footer), height - 8 * density, footerPaint)
        return out
    }

    /** Writes the PNG into cache/shared_images and opens Android's share sheet. */
    fun share(context: Context, image: Bitmap, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest share
        val file = File(dir, "graph_${System.currentTimeMillis()}.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Typed text made readable without LaTeX: x^2 → x², v_0 → v₀, ∫_0^3 → ∫₀³, sqrt → √, <= → ≤, * → ·.
     * Integral bounds that have no small-letter form (∫_0^π) become ∫[0, π].
     */
    fun pretty(text: String): String {
        var s = text.replace("sqrt", "√").replace("<=", "≤").replace(">=", "≥").replace("*", "·")
            .replace("pi", "π").replace("theta", "θ")
        s = INTEGRAL.replace(s) { m ->
            val a = unwrap(m.groupValues[1])
            val b = unwrap(m.groupValues[2])
            val sub = small(a, SUB)
            val sup = small(b, SUP)
            if (sub != null && sup != null) "∫$sub$sup" else "∫[$a, $b]"
        }
        s = SUBSCRIPT.replace(s) { m -> small(unwrap(m.groupValues[1]), SUB) ?: m.value }
        s = Regex("""\^(-?)(\d+)(?![.\d])""").replace(s) { m ->
            (if (m.groupValues[1].isNotEmpty()) "⁻" else "") + m.groupValues[2].map { SUP.getValue(it) }.joinToString("")
        }
        return s
    }

    private const val TOKEN = """(\([^()]*\)|-?[\p{L}\d.]+)"""
    private val INTEGRAL = Regex("""∫_$TOKEN\^$TOKEN""")
    private val SUBSCRIPT = Regex("""(?<=[\p{L}\d)])_$TOKEN""")

    private fun unwrap(token: String) = if (token.startsWith("(") && token.endsWith(")")) token.substring(1, token.length - 1) else token

    /** The text in sub/superscript characters, or null when one of them has no such form. */
    private fun small(text: String, table: Map<Char, Char>): String? =
        if (text.isNotEmpty() && text.all { it in table }) text.map { table.getValue(it) }.joinToString("") else null

    private val SUP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '-' to '⁻', '+' to '⁺', 'n' to 'ⁿ', 'i' to 'ⁱ',
    )
    private val SUB = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '-' to '₋', '+' to '₊', 'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'i' to 'ᵢ', 'n' to 'ₙ', 'm' to 'ₘ', 'k' to 'ₖ',
        't' to 'ₜ', 'r' to 'ᵣ', 's' to 'ₛ', 'p' to 'ₚ', 'h' to 'ₕ', 'l' to 'ₗ', 'j' to 'ⱼ', 'u' to 'ᵤ', 'v' to 'ᵥ',
    )
}
