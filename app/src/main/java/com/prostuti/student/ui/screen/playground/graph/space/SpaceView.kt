package com.prostuti.student.ui.screen.playground.graph.space

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.prostuti.student.ui.screen.playground.graph.share.GraphCapture
import com.prostuti.student.ui.screen.playground.graph.share.recordInto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prostuti.student.domain.graph.GraphNumberFormat
import com.prostuti.student.domain.graph.plot.GridTicks
import com.prostuti.student.domain.graph.space.SpaceAxes
import com.prostuti.student.domain.graph.space.SpaceGeometry
import com.prostuti.student.ui.screen.playground.graph.GraphColors
import com.prostuti.student.ui.screen.playground.graph.GraphLineState
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * The 3D graph: an OpenGL view underneath, labels drawn on top, and a gesture layer that turns
 * the camera (one finger), zooms (pinch) and resets (double tap). With [SpaceSettings.gyroscope]
 * on, turning the phone turns the graph.
 */
@Composable
fun SpaceView(
    geometries: List<SpaceGeometry>,
    lines: List<GraphLineState>,
    settings: SpaceSettings,
    camera: SpaceCamera,
    range: Double,
    contentDescription: String,
    onRotate: (dYaw: Float, dPitch: Float) -> Unit,
    onZoom: (factor: Float) -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier,
    capture: GraphCapture? = null,
) {
    val overlayLayer = rememberGraphicsLayer()
    SideEffect { capture?.overlayLayer = overlayLayer }
    val palette = GraphColors.palette()
    val scheme = MaterialTheme.colorScheme
    val background = scheme.surface
    val ink = scheme.onSurface
    val axisColors = remember(ink) { listOf(Color(0xFFD9534F), Color(0xFF3FA34D), Color(0xFF3A7BD5)) }
    // Axes are rebuilt only when the zoom moves to a new "decade" bucket; in between they stretch.
    val axesRange = remember(range) { bucket(range) }
    val axes = remember(axesRange) { SpaceAxes.build(axesRange) }

    val scene = remember(geometries, lines, settings, camera, range, palette, background, axes) {
        buildScene(geometries, lines, settings, camera, range, palette, background, ink, axes, axisColors)
    }
    val renderer = remember { SpaceRenderer() }
    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }
    val density = LocalDensity.current.density
    renderer.lineWidthPx = 1.1f * density

    LifecycleAware(glView)
    Gyroscope(settings.gyroscope, onRotate)

    val rotate by rememberUpdatedState(onRotate)
    val zoom by rememberUpdatedState(onZoom)
    val reset by rememberUpdatedState(onResetCamera)
    val measurer = rememberTextMeasurer()

    Box(modifier.testTag("space_view").semantics { this.contentDescription = contentDescription }) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3)
                    setEGLConfigChooser(MultisampleConfigChooser())
                    preserveEGLContextOnPause = true
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    glView = this
                    capture?.glView = this
                }
            },
            update = { view ->
                renderer.scene = scene
                view.requestRender()
            },
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .recordInto(overlayLayer)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, factor, _ ->
                        val degreesPerPx = 200f / size.width.coerceAtLeast(1)
                        rotate(-pan.x * degreesPerPx, pan.y * degreesPerPx)
                        if (factor != 1f) zoom(factor)
                    }
                }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { reset() }) },
        ) {
            val w = size.width.toInt()
            val h = size.height.toInt()
            val mvp = camera.mvp(w, h, range)
            val style = TextStyle(fontSize = 11.sp, color = ink.copy(alpha = 0.8f))
            fun label(text: String, x: Double, y: Double, z: Double, color: Color = ink, size: Int = 11) {
                val p = camera.project(mvp, x, y, z, w, h) ?: return
                val layout = measurer.measure(text, style.copy(color = color, fontSize = size.sp))
                drawText(layout, topLeft = Offset(p[0] - layout.size.width / 2f, p[1] - layout.size.height / 2f))
            }
            if (settings.showAxes) {
                listOf("x", "y", "z").forEachIndexed { i, name ->
                    val tip = DoubleArray(3).also { it[i] = range * 1.24 }
                    label(name, tip[0], tip[1], tip[2], axisColors[i], 15)
                }
                if (settings.showNumbers) {
                    val step = GridTicks.step(2 * range / 8, 1.0).major
                    for (v in GridTicks.ticks(-range, range, step)) {
                        if (abs(v) < step * 1e-6) continue
                        val text = GraphNumberFormat.format(v, 4)
                        val faint = ink.copy(alpha = 0.55f)
                        label(text, v, -range * 0.06, 0.0, faint)
                        label(text, -range * 0.06, v, 0.0, faint)
                        label(text, -range * 0.06, -range * 0.06, v, faint)
                    }
                }
            }
            for (g in geometries) {
                val line = lines.firstOrNull { it.id == g.lineId } ?: continue
                if (!line.visible) continue
                for (p in g.labels) {
                    val text = "(${GraphNumberFormat.format(p[0], 4)}, ${GraphNumberFormat.format(p[1], 4)}, ${GraphNumberFormat.format(p[2], 4)})"
                    label(text, p[0], p[1], p[2] + range * 0.08, palette[Math.floorMod(line.colorIndex, palette.size)])
                }
            }
        }
    }
}

/** Nearest "nice" range at or above [range] on a ×2 ladder, so axes are rebuilt rarely. */
private fun bucket(range: Double): Double {
    val k = round(ln(range) / ln(2.0))
    return 2.0.pow(k)
}

private fun buildScene(
    geometries: List<SpaceGeometry>,
    lines: List<GraphLineState>,
    settings: SpaceSettings,
    camera: SpaceCamera,
    range: Double,
    palette: List<Color>,
    background: Color,
    ink: Color,
    axes: SpaceAxes,
    axisColors: List<Color>,
): SpaceScene {
    val items = ArrayList<SceneItem>()
    val axesScale = (range / axes.range).toFloat()
    if (settings.showAxes) {
        axes.axes.forEachIndexed { i, mesh -> items += SceneItem(mesh, null, axisColors[i].toArgb(), clip = false, scale = axesScale) }
    }
    if (settings.showBox) items += SceneItem(null, axes.box, ink.toArgb(), lineAlpha = 0.28f, clip = false, scale = axesScale)
    if (settings.showGrid) items += SceneItem(null, axes.grid, ink.toArgb(), lineAlpha = 0.10f, scale = axesScale)
    for (g in geometries) {
        val line = lines.firstOrNull { it.id == g.lineId } ?: continue
        if (!line.visible) continue
        val color = palette[Math.floorMod(line.colorIndex, palette.size)].toArgb()
        val options = line.options
        val opacity = options.opacity.toFloat().coerceIn(0.1f, 1f)
        for (mesh in g.surfaces) {
            items += SceneItem(mesh, null, color, opacity, heightColor = options.colorByHeight, heightMin = g.heightMin, heightMax = g.heightMax)
        }
        for (mesh in g.solidParts) items += SceneItem(mesh, null, color)
        g.lines?.let { items += SceneItem(null, it, ink.toArgb(), lineAlpha = 0.30f) }
    }
    return SpaceScene(items, camera, range, background.toArgb())
}

/** GLSurfaceView must pause and resume with the screen. */
@Composable
private fun LifecycleAware(view: GLSurfaceView?) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/** While [enabled], the phone's gyroscope turns the camera (degrees per rotation of the device). */
@Composable
private fun Gyroscope(enabled: Boolean, onRotate: (Float, Float) -> Unit) {
    val context = LocalContext.current
    val rotate by rememberUpdatedState(onRotate)
    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (!enabled || manager == null || sensor == null) return@DisposableEffect onDispose { }
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (last != 0L) {
                    val dt = (event.timestamp - last) / 1e9f
                    val toDeg = 180f / Math.PI.toFloat()
                    // Tilting left/right (device y-axis) turns the graph around; forward/back (x-axis) lifts it.
                    rotate(-event.values[1] * dt * toDeg, event.values[0] * dt * toDeg)
                }
                last = event.timestamp
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
}
