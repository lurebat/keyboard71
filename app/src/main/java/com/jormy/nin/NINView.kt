package com.jormy.nin

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.EGL14
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import com.jormy.nin.NINLib.init
import com.jormy.nin.NINLib.onTouchEvent
import com.jormy.nin.NINLib.step
import com.jormy.nin.NINLib.syncTiming
import com.jormy.nin.SoftKeyboard.Companion.relayDelayedEvents
import com.jormy.nin.Utils.prin
import com.jormy.nin.Utils.tracedims
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

class NINView(context: Context) : EXSurfaceView(context) {
    var desiredRoenFullscreen = false
    var desiredRoenPixelHeight = 800.0f
    var desiredRoenPixelWidth = 640.0f
    var xViewScaling = 1.0f
    var yViewScaling = 1.0f
    private val preferences = Preferences(context)
    private val gestureDetector = GestureDetector(getContext(), object : SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            if (preferences.hapticFeedbackBlocking()) {
                this@NINView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    })

    init {
        syncTiming(System.currentTimeMillis())
        movementEventsQueue = ConcurrentLinkedQueue()
        holder.setFormat(PixelFormat.TRANSLUCENT)
        if (globalContextFactory == null) {
            globalContextFactory = ContextFactory()
        }
        setEGLContextFactory(globalContextFactory)
        setEGLConfigChooser(ConfigChooser(8, 8, 8, 8, 0, 0))
        setRenderer(Renderer())
        setPreserveEGLContextOnPause(true)
        globalView = this
        val metrics = resources.displayMetrics
        devicePPI = metrics.xdpi
        devicePortraitWidth = Math.min(metrics.widthPixels, metrics.heightPixels).toFloat()
        onResume()
    }

    class ContextFactory : EGLContextFactory {
        // com.jormy.nin.EXSurfaceView.EGLContextFactory
        override fun createContext(
            egl10: EGL10,
            display: EGLDisplay,
            eglConfig: EGLConfig
        ): EGLContext {
            if (eglContext == null) {
                Log.w(TAG, "creating OpenGL ES 2.0 context")
                checkEglError("Before eglCreateContext", egl10)
                val context = egl10.eglCreateContext(
                    display,
                    eglConfig,
                    EGL10.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                )
                checkEglError("After eglCreateContext", egl10)
                eglContext = context
                eglDisplay = display
                return context
            }
            if (display !== eglDisplay) {
                prin("BUT THE DISPLAY IS FUCKING DIFFERENT!")
                Thread.currentThread()
                Thread.dumpStack()
                System.exit(1)
            }
            return eglContext!!
        }

        // com.jormy.nin.EXSurfaceView.EGLContextFactory
        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            prin("--------------------- Destroy context, but nope")
            prin("Exiting, because we need to recreate the OGL Context!")
            egl.eglDestroyContext(display, context)
            Thread.currentThread()
            Thread.dumpStack()
            System.exit(1)
        }

        companion object {
            var eglContext: EGLContext? = null
            var eglDisplay: EGLDisplay? = null
        }
    }

    data class ConfigChooser(
        val redSize: Int,
        val greenSize: Int,
        val blueSize: Int,
        val alphaSize: Int,
        val minDepthSize: Int,
        val minStencilSize: Int
    ) : EGLConfigChooser {
        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val numConfigsArr = IntArray(1)
            egl.eglChooseConfig(display, attributes, null, 0, numConfigsArr)
            val numConfigs = numConfigsArr[0]
            require(numConfigs > 0) { "No configs match configSpec" }
            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            egl.eglChooseConfig(display, attributes, configs, numConfigs, numConfigsArr)
            return chooseConfig(egl, display, configs)
        }

        private fun chooseConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig {
            for (config in configs) {
                val d = egl.findConfigAttrib(display, config, EGL14.EGL_DEPTH_SIZE)
                val s = egl.findConfigAttrib(display, config, EGL14.EGL_STENCIL_SIZE)
                if (d < minDepthSize || s < minStencilSize) continue

                val r = egl.findConfigAttrib(display, config, EGL14.EGL_RED_SIZE)
                val g = egl.findConfigAttrib(display, config, EGL14.EGL_GREEN_SIZE)
                val b = egl.findConfigAttrib(display, config, EGL14.EGL_BLUE_SIZE)
                val a = egl.findConfigAttrib(display, config, EGL14.EGL_ALPHA_SIZE)
                if (r != redSize || g != greenSize || b != blueSize || a != alphaSize) continue

                if (config == null) continue

                return config
            }
            throw IllegalArgumentException("No config chosen")
        }

        private fun EGL10.findConfigAttrib(
            display: EGLDisplay,
            config: EGLConfig?,
            attribute: Int,
        ): Int = intArrayOf(0).let {
            return if (eglGetConfigAttrib(display, config, attribute, it)) {
                it[0]
            } else {
                0
            }
        }

        companion object {
            private val attributes = intArrayOf(
                EGL14.EGL_RED_SIZE,
                4,
                EGL14.EGL_GREEN_SIZE,
                4,
                EGL14.EGL_BLUE_SIZE,
                4,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
        }
    }

    val desiredPixelWidth: Float
        get() {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()
            val ppi = metrics.xdpi
            val portraitPixels = min(width, height)
            val portraitInches = portraitPixels / ppi
            val pixelPerfectRoenPixelWidth = desiredRoenPixelWidth * (portraitInches / 1.9631902f)
            val desiredPortrait = pixelPerfectRoenPixelWidth / desiredScaling
            lastDesiredPortrait = desiredPortrait
            return if (width <= height) desiredPortrait else desiredPortrait * (width / height)
        }

    // android.view.View
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val actionId = event.actionMasked
        val pointerCount = event.pointerCount
        for (i in 0 until pointerCount) {
            doHapticFeedback(actionId)
            if ((actionId == MotionEvent.ACTION_POINTER_UP || actionId == MotionEvent.ACTION_POINTER_DOWN) && i != event.actionIndex) {
                continue
            }
            movementEventsQueue.add(
                RelayTouchInfo(
                    event.getPointerId(i),
                    event.getX(i) / xViewScaling,
                    event.getY(i) / yViewScaling,
                    event.getPressure(i),
                    event.getSize(i),
                    System.currentTimeMillis(),
                    actionToJormyAction(actionId)
                )
            )
        }
        globalView.requestRender()
        return true
    }

    private fun doHapticFeedback(actionId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return
        }
        if (!preferences.hapticFeedbackBlocking()) {
            return
        }
        if (actionId == MotionEvent.ACTION_DOWN || actionId == MotionEvent.ACTION_POINTER_DOWN) {
            this.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // android.view.SurfaceView, android.view.View
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val metrics = resources.displayMetrics
        val wwww = (metrics.widthPixels + 2).toFloat()
        val height = (metrics.heightPixels + 2).toFloat()
        tracedims(
            "::::::::onMeasure called --------------- : ",
            widthMeasureSpec.toFloat(),
            heightMeasureSpec.toFloat()
        )
        tracedims("metrics : ", metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
        val desiredWidth = desiredPixelWidth
        val actual_scale = desiredWidth / wwww
        prin("desired width : $desiredWidth")
        var rawRoenDesiredHeight = desiredRoenPixelHeight
        if (!desiredRoenFullscreen) {
            rawRoenDesiredHeight = desiredRoenPixelHeight
        }
        prin("rawRoenDesiredHeight : $rawRoenDesiredHeight")
        prin("actual scaling : $actual_scale")
        val desiredHeight = Math.min(rawRoenDesiredHeight, height / wwww * desiredWidth)
        prin("desiredHeight : $desiredHeight")
        val f = wwww / desiredWidth
        yViewScaling = f
        xViewScaling = f
        val height2 = wwww * desiredHeight / desiredWidth
        val holder = holder
        holder?.setFixedSize(desiredWidth.toInt(), desiredHeight.toInt())
        tracedims("setMeasuredDims : ", wwww, height2)
        setMeasuredDimension(wwww.toInt(), height2.toInt())
    }

    class Renderer : EXSurfaceView.Renderer {
        override fun onDrawFrame(gl10: GL10) {
            while (true) {
                val rti = movementEventsQueue.poll()
                if (rti != null) {
                    onTouchEvent(
                        rti.touchid,
                        rti.jormactionid,
                        rti.xPos,
                        rti.yPos,
                        rti.pressureValue,
                        rti.areaValue,
                        rti.timestamp_long
                    )
                } else {
                    relayDelayedEvents()
                    step()
                    return
                }
            }
        }

        // com.jormy.nin.EXSurfaceView.Renderer
        override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
            val metrics = globalView.resources.displayMetrics
            val widthPixels = metrics.widthPixels
            val heightPixels = metrics.heightPixels
            init(width, height, widthPixels, heightPixels)
        }

        override fun onSurfaceCreated(gl10: GL10, config: EGLConfig) {}
    }

    companion object {
        var globalContextFactory: ContextFactory? = null
        lateinit var globalView: NINView
        var movementEventsQueue: ConcurrentLinkedQueue<RelayTouchInfo> = ConcurrentLinkedQueue()
        private const val TAG = "NINView"

        @get:Api
        @JvmStatic
        var devicePPI = 326.0f

        @get:Api
        @JvmStatic
        var devicePortraitWidth = 640.0f

        var desiredScaling = 1.0f
        var lastDesiredPortrait = 640.0f

        fun checkEglError(prompt: String?, egl: EGL10) {
            while (true) {
                when (egl.eglGetError()) {
                    EGL14.EGL_SUCCESS -> {
                        return
                    }
                    else -> {
                        Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, egl.eglGetError()))
                    }
                }
            }
        }

        fun actionToJormyAction(action: Int): Int {
            return when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> 2
                MotionEvent.ACTION_MOVE -> 1
                else -> -1
            }
        }

        @Api
        @JvmStatic
        fun adjustWantedScaling(scaling: Float) {
            desiredScaling = scaling
            Handler(Looper.getMainLooper()).post { globalView.requestLayout() }
        }

        @Api
        @JvmStatic
        fun onRoenSignalDirty() {
            globalView.requestRender()
        }

        @Api
        @JvmStatic
        fun onRoenFrozennessChange(truth: Boolean) {
            if (!truth) {
                globalView.requestRender()
            }
        }

        @Api
        @JvmStatic
        fun adjustKeyboardDimensions(wantedRoenHeight: Float, fullscreen: Boolean) {
            globalView.desiredRoenPixelHeight = 2.0f * wantedRoenHeight
            globalView.desiredRoenFullscreen = fullscreen
            prin("::::::::::: onAdjustKeyboardDimension : $wantedRoenHeight // $fullscreen")
            if (fullscreen) {
                val metrics = globalView.resources.displayMetrics
                val wwww = metrics.widthPixels.toFloat()
                val height = (metrics.heightPixels - 240).toFloat()
                val wantedratio = height / wwww
                tracedims("fullmode metrics, after cut", wwww, height)
                val desiredpixwidth = globalView.desiredPixelWidth
                globalView.desiredRoenPixelHeight = desiredpixwidth * wantedratio
                prin("Desired pixwidth : $desiredpixwidth")
            }
            prin("what is scaling: " + desiredScaling)
            if (!fullscreen) {
                prin("Roenpixheight : " + globalView.desiredRoenPixelHeight + " from " + wantedRoenHeight)
            } else {
                prin("Roenpixheight : " + globalView.desiredRoenPixelHeight)
            }
            // from class: com.jormy.nin.NINView.3
// java.lang.Runnable
            Handler(Looper.getMainLooper()).post { globalView.requestLayout() }
        }
    }
}
