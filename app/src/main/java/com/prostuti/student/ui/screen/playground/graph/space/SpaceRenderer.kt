package com.prostuti.student.ui.screen.playground.graph.space

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.IdentityHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Draws a [SpaceScene] with OpenGL ES 3. Meshes are uploaded once (keyed by their array identity)
 * and freed when they leave the scene, so turning the view only changes a few uniforms.
 *
 * Pass order: solid meshes (pushed back slightly so lines on them don't flicker), then thin lines,
 * then see-through meshes without depth writes.
 */
internal class SpaceRenderer : GLSurfaceView.Renderer {

    @Volatile var scene: SpaceScene? = null
    @Volatile var lineWidthPx: Float = 2f

    private var program = 0
    private var uMvp = 0
    private var uScale = 0
    private var uColor = 0
    private var uBox = 0
    private var uLit = 0
    private var uHeight = 0
    private var uHeightRange = 0
    private var uLightDir = 0
    private var uEye = 0
    private var width = 1
    private var height = 1
    private var maxLineWidth = 1f

    private val buffers = IdentityHashMap<FloatArray, Int>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        buffers.clear() // a new context: old buffer ids are gone
        program = link(SpaceShaders.VERTEX, SpaceShaders.FRAGMENT)
        uMvp = GLES30.glGetUniformLocation(program, "uMvp")
        uScale = GLES30.glGetUniformLocation(program, "uScale")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uBox = GLES30.glGetUniformLocation(program, "uBox")
        uLit = GLES30.glGetUniformLocation(program, "uLit")
        uHeight = GLES30.glGetUniformLocation(program, "uHeight")
        uHeightRange = GLES30.glGetUniformLocation(program, "uHeightRange")
        uLightDir = GLES30.glGetUniformLocation(program, "uLightDir")
        uEye = GLES30.glGetUniformLocation(program, "uEye")
        val range = FloatArray(2)
        GLES30.glGetFloatv(GLES30.GL_ALIASED_LINE_WIDTH_RANGE, range, 0)
        maxLineWidth = range[1].coerceAtLeast(1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val s = scene
        val bg = s?.background ?: 0xFFFFFFFF.toInt()
        GLES30.glClearColor(r(bg), g(bg), b(bg), 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (s == null || program == 0) return
        releaseUnused(s)

        val mvp = s.camera.mvp(width, height, s.range)
        val eyeDir = s.camera.eyeDirection()
        val distance = SpaceCamera.DISTANCE * s.range.toFloat()
        val right = Mat4.normalize(Mat4.cross(floatArrayOf(-eyeDir[0], -eyeDir[1], -eyeDir[2]), floatArrayOf(0f, 0f, 1f)))
        val light = Mat4.normalize(
            floatArrayOf(
                eyeDir[0] * 0.75f - right[0] * 0.35f,
                eyeDir[1] * 0.75f - right[1] * 0.35f,
                eyeDir[2] * 0.75f - right[2] * 0.35f + 0.55f,
            ),
        )
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES30.glUniform3f(uLightDir, light[0], light[1], light[2])
        GLES30.glUniform3f(uEye, eyeDir[0] * distance, eyeDir[1] * distance, eyeDir[2] * distance)
        GLES30.glLineWidth(lineWidthPx.coerceIn(1f, maxLineWidth))

        // 1. solid meshes
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glPolygonOffset(1f, 1f)
        for (item in s.items) if (item.mesh != null && item.opacity >= 0.999f) drawMesh(item, s, opaque = true)
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)

        // 2. lines
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glDepthMask(false)
        for (item in s.items) if (item.lines != null) drawLines(item, s)

        // 3. see-through meshes
        for (item in s.items) if (item.mesh != null && item.opacity < 0.999f) drawMesh(item, s, opaque = false)
        GLES30.glDepthMask(true)
    }

    private fun common(item: SceneItem, s: SpaceScene, alpha: Float) {
        GLES30.glUniform1f(uScale, item.scale)
        GLES30.glUniform4f(uColor, r(item.color), g(item.color), b(item.color), alpha)
        GLES30.glUniform1f(uBox, if (item.clip) (s.range * 1.0005).toFloat() else 0f)
        GLES30.glUniform1i(uHeight, if (item.heightColor) 1 else 0)
        GLES30.glUniform2f(uHeightRange, item.heightMin, item.heightMax)
    }

    private fun drawMesh(item: SceneItem, s: SpaceScene, opaque: Boolean) {
        val mesh = item.mesh ?: return
        if (mesh.isEmpty) return
        common(item, s, if (opaque) 1f else item.opacity)
        GLES30.glUniform1i(uLit, if (item.lit) 1 else 0)
        bind(0, mesh.positions)
        bind(1, mesh.normals)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mesh.vertexCount)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun drawLines(item: SceneItem, s: SpaceScene) {
        val lines = item.lines ?: return
        if (lines.isEmpty) return
        common(item, s, item.lineAlpha)
        GLES30.glUniform1i(uLit, 0)
        GLES30.glUniform1i(uHeight, 0)
        bind(0, lines.positions)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glVertexAttrib3f(1, 0f, 0f, 1f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, lines.positions.size / 3)
        GLES30.glDisableVertexAttribArray(0)
    }

    private fun bind(location: Int, data: FloatArray) {
        val vbo = buffers.getOrPut(data) { upload(data) }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glEnableVertexAttribArray(location)
        GLES30.glVertexAttribPointer(location, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun upload(data: FloatArray): Int {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        val buffer = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(data).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buffer, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        return ids[0]
    }

    /** Frees GPU buffers of meshes that are no longer in the scene. */
    private fun releaseUnused(s: SpaceScene) {
        if (buffers.isEmpty()) return
        val live = IdentityHashMap<FloatArray, Boolean>()
        for (item in s.items) {
            item.mesh?.let { live[it.positions] = true; live[it.normals] = true }
            item.lines?.let { live[it.positions] = true }
        }
        val dead = buffers.keys.filter { it !in live }
        if (dead.isEmpty()) return
        val ids = dead.map { buffers.remove(it)!! }.toIntArray()
        GLES30.glDeleteBuffers(ids.size, ids, 0)
    }

    private fun link(vertexSource: String, fragmentSource: String): Int {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, vs)
        GLES30.glAttachShader(p, fs)
        GLES30.glLinkProgram(p)
        val status = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Timber.e("3D program link failed: %s", GLES30.glGetProgramInfoLog(p))
            return 0
        }
        return p
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) Timber.e("3D shader compile failed: %s", GLES30.glGetShaderInfoLog(shader))
        return shader
    }

    private fun r(c: Int) = ((c shr 16) and 0xFF) / 255f
    private fun g(c: Int) = ((c shr 8) and 0xFF) / 255f
    private fun b(c: Int) = (c and 0xFF) / 255f
}
