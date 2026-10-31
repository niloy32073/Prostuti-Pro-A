package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.prostuti.student.domain.graph.plot.LineGeometry

/** A [LineGeometry] turned into Compose paths once, then redrawn every frame with a transform. */
internal class GeometryPaths(geometry: LineGeometry) {
    val curves: Path = polylines(geometry.curves)
    val segments: Path = Path().apply {
        geometry.segments?.let { s ->
            for (i in s.indices step 4) {
                moveTo(s[i], s[i + 1])
                lineTo(s[i + 2], s[i + 3])
            }
        }
    }
    val fills: Path = polygons(geometry.fills).apply {
        geometry.fillRects?.let { r ->
            for (i in r.indices step 4) addRect(Rect(r[i], r[i + 1], r[i + 2], r[i + 3]))
        }
    }
    val negativeFills: Path = polygons(geometry.negativeFills)
    val derivative: Path = polylines(geometry.derivativeCurves)
    val riemann: Path = polygons(geometry.riemannPolygons)
    val guides: Path = Path().apply {
        geometry.guides?.let { g ->
            for (i in g.indices step 4) {
                moveTo(g[i], g[i + 1])
                lineTo(g[i + 2], g[i + 3])
            }
        }
    }
    val tangent: Path = Path().apply {
        geometry.tangent?.line?.let { t -> moveTo(t[0], t[1]); lineTo(t[2], t[3]) }
    }
    val hasFill = geometry.fills.isNotEmpty() || (geometry.fillRects?.isNotEmpty() == true)
    val hasNegative = geometry.negativeFills.isNotEmpty()
    val hasRiemann = geometry.riemannPolygons.isNotEmpty()

    private fun polylines(lines: List<FloatArray>) = Path().apply {
        for (c in lines) {
            if (c.size < 4) continue
            moveTo(c[0], c[1])
            for (i in 2 until c.size step 2) lineTo(c[i], c[i + 1])
        }
    }

    private fun polygons(polys: List<FloatArray>) = Path().apply {
        for (p in polys) {
            if (p.size < 6) continue
            moveTo(p[0], p[1])
            for (i in 2 until p.size step 2) lineTo(p[i], p[i + 1])
            close()
        }
    }
}
