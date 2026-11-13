package com.prostuti.student.ui.screen.playground.graph.space

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/** Asks for 4× MSAA (smooth edges on curves and surfaces); falls back to no MSAA if unsupported. */
internal class MultisampleConfigChooser : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        return choose(egl, display, samples = 4) ?: choose(egl, display, samples = 0)
            ?: error("No OpenGL ES 3 configuration")
    }

    private fun choose(egl: EGL10, display: EGLDisplay, samples: Int): EGLConfig? {
        val base = intArrayOf(
            EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16, EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        )
        val multisample = if (samples > 0) intArrayOf(EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, samples) else IntArray(0)
        val attributes = base + multisample + intArrayOf(EGL10.EGL_NONE)
        val count = IntArray(1)
        if (!egl.eglChooseConfig(display, attributes, null, 0, count) || count[0] == 0) return null
        val configs = arrayOfNulls<EGLConfig>(count[0])
        egl.eglChooseConfig(display, attributes, configs, count[0], count)
        return configs.firstOrNull()
    }

    private companion object {
        const val EGL_RENDERABLE_TYPE = 0x3040
        const val EGL_OPENGL_ES3_BIT = 0x40
    }
}
