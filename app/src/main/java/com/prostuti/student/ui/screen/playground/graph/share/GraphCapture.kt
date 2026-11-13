package com.prostuti.student.ui.screen.playground.graph.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.Modifier
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Takes a picture of the graph as it is on screen. 2D is recorded from Compose; 3D comes from the
 * OpenGL surface through PixelCopy (Compose can't see GL pixels), with the label overlay on top.
 */
class GraphCapture {
    internal var planeLayer: GraphicsLayer? = null
    internal var overlayLayer: GraphicsLayer? = null
    internal var glView: GLSurfaceView? = null

    suspend fun capture(space: Boolean): Bitmap? = if (space) captureSpace() else captureLayer(planeLayer)

    private suspend fun captureLayer(layer: GraphicsLayer?): Bitmap? {
        val image = layer?.takeIf { it.size.width > 0 && it.size.height > 0 }?.toImageBitmap() ?: return null
        // Hardware bitmaps can't be drawn into the share canvas: copy to a normal one.
        return image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    }

    private suspend fun captureSpace(): Bitmap? {
        val view = glView ?: return null
        if (view.width <= 0 || view.height <= 0) return null
        val bitmap = createBitmap(view.width, view.height)
        val ok = suspendCancellableCoroutine { cont ->
            PixelCopy.request(view, bitmap, { result -> cont.resume(result == PixelCopy.SUCCESS) }, Handler(Looper.getMainLooper()))
        }
        if (!ok) return null
        captureLayer(overlayLayer)?.let { labels -> Canvas(bitmap).drawBitmap(labels, 0f, 0f, null) }
        return bitmap
    }
}

/** Draws as usual and also records the drawing into [layer], so it can be turned into a bitmap. */
fun Modifier.recordInto(layer: GraphicsLayer?): Modifier =
    if (layer == null) this else drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
